#!/usr/bin/env bash

set -eo pipefail

DEBUG=""
FORCE=""
progName=$(basename $0)
componentBuildPath="_all"
componentTemplatePath=""
declare -a envs yenvs yparams jparams
environment=""
declare -A cliparam
cliparamJson=""

cfnSubfolder="cfn"

parse_args() {
  while getopts ":e:fdc:p:" opt; do
    case "${opt}" in
      e)
        if [[ "$OPTARG" == "" ]]; then
          echo "-e needs an environment" >&2
          exit 1
        fi
        envs+=($OPTARG)
        ;;
      f)
        FORCE="ON"
        ;;
      d)
        DEBUG="ON"
        ;;
      c)
        if [[ "$OPTARG" == "" ]]; then
          echo "-c needs a template folder (component)" >&2
          exit 1
        fi
        componentTemplatePath=$OPTARG
        componentBuildPath=$OPTARG
        ;;
      p)
        if [[ "$OPTARG" == "" ]]; then
          echo "-p needs a key=value pair" >&2
          exit 1
        fi
        key=$(echo $OPTARG|cut -f1 -d"=")
        value=$(echo $OPTARG|cut -f2 -d"=")
        [[ -z "$key" || -z "$value" ]] && echo "invalid parameter given: -$opt $OPTARG" && exit 1
        cliparam[$key]=$value
        ;;
      *)
        echo "Invalid argument passed: -$OPTARG" >&2
        sub_help
        exit 1
        ;;
    esac
  done
}

sub_help() {
    echo "Usage: $progName <subcommand> -e ENVIRONMENT [-e ENVIRONMENT...] [-c COMPONENT] [-p key=value ] [-d] [-f]"
    echo ""
    echo "Subcommands:"
    echo "    clean     Clean the compile folder (under _build)"
    echo "    compile   Use gomplate to compile the templates"
    echo "    validate  Validate the compiled templates against a Kubernetes API server"
    echo "    deploy    Apply the compiled templates to a Kubernetes API server"
    echo "    delete    Delete the items in the compiled templates on a Kubernetes API server"
    echo "Flags:"
    echo "    -d        DEBUG mode"
    echo "    -f        FORCE mode: Ignore errors when trying to delete Kubernetes objects. For delete command only."
    echo ""
}

sub_clean() {
  rm -rf _build
}

is_empty_file() {
  if [ -s $1 ]; then
    # File exists and the content with all whitespace
    grep -q '[^[:space:]]' < $1 && echo "False" || echo "True"
  else
    # File doesn't exist or is empty
    echo "True"
  fi
}

compile_environment() {

  echo "compiling environment: ${envs[@]}"
  for env in "${envs[@]}"; do
    if [ -f "envs/$env.yaml" ]; then
      yenvs+=($env)
      yparams+=(envs/$env.yaml)
    fi
    environment=$env
  done

  # we have valid environment files
  # the last provided environment is what we use to name things with
  mkdir -p _build/$environment

  # build the cli parameter input file
  cparams=_build/$environment/cliParams.json
  cliparamJson=$(for i in "${!cliparam[@]}"
  do
    echo "$i"
    echo "${cliparam[$i]}"
  done |
  jq -cn -R 'reduce inputs as $param ({}; . + setpath($param | split("."); input))')
  if [ -n "$cliparamJson" ]; then
    echo "cli parameters provided: $cliparamJson"
    echo "$cliparamJson" > $cparams
  else
    echo "no overrides"
  fi
  touch $cparams

  indices=${!yenvs[*]}
  for i in $indices; do
    jparams[$i]=_build/$environment/${yenvs[$i]}.json
    cat ${yparams[$i]} | yaml2json > ${jparams[$i]}
  done
  # merge all environs
  cat ${jparams[@]} $cparams | jq --slurp 'reduce .[] as $item ({}; . * $item)' > _build/$environment/parameters.json

}

sub_compile() {

  if [ ! -d "./templates/$componentTemplatePath" ]; then
    echo "The component directory templates/$componentTemplatePath does not exist!" >&2
    exit 2
  fi

  if [ -z "$environment" ]; then
    echo "No environment is set!" >&2
    exit 2
  fi

  gomplate \
    --output-dir _build/$environment/$componentBuildPath/templates \
    --input-dir templates/$componentTemplatePath \
    --datasource config=_build/$environment/parameters.json
}

sub_validate() {
  sub_compile

  # TODO: create a change-set in stackup for validation?
  find _build/$environment/$componentBuildPath/templates/ -type f -name "*.yaml" \
    | grep -v "/$cfnSubfolder/" \
    | sort \
    | xargs -n1 kubectl apply --validate --dry-run -f
}

sub_deploy() {
  sub_compile

  for f in $(find _build/$environment/$componentBuildPath/templates/ -type f -name "*.yaml" -path "*/$cfnSubfolder/*" | sort); do
    if [ $(is_empty_file $f) != "True" ]; then
      stackup $environment-$(basename $f .yaml) up -t $f
    fi
  done

  for f in $(find _build/$environment/$componentBuildPath/templates/ -type f -name "*.yaml" \
    | grep -v "/$cfnSubfolder/" \
    | sort); do
    if [ $(is_empty_file $f) != "True" ]; then
      kubectl apply -f $f
    fi
  done
}

sub_delete() {
  sub_compile
  for f in $(find _build/$environment/$componentBuildPath/templates/ -type f -name "*.yaml" \
    | grep -v "/$cfnSubfolder/" \
    | sort -r); do
    if [ $(is_empty_file $f) != "True" ]; then
      if [ -n "$FORCE" ]; then
        kubectl delete -f $f > /dev/null 2>&1 || true
      else
        kubectl delete -f $f
      fi
    fi
  done
  for f in $(find _build/$environment/$componentBuildPath/templates/ -type f -path "*/$cfnSubfolder/*" -name "*.yaml" | sort); do
    stackup $environment-$(basename $f .yaml) down
  done
}


subcommand=$1
case $subcommand in
    "" | "-h" | "--help")
        sub_help
        ;;
    *)
        shift
        parse_args $@
        shift $((OPTIND-1))

        if [ ${#envs[@]} -ge 1 ]; then
          compile_environment
          echo "environment: $environment"
        fi

        echo "componentBuildPath: $componentBuildPath"
        echo "componentTemplatePath: $componentTemplatePath"
        echo "command: $subcommand"
        if [ -n "$FORCE" ]; then
          echo "--- FORCE MODE ON ---"
          echo "Delete commands will ignore errors"
          echo ""
        fi
        if [ -n "$DEBUG" ]; then
          echo "--- DEBUG MODE ON ---"
          echo "The following commands would have been run without the -d switch:"
          echo ""

          rm() {
            echo rm $@
          }

          kubectl() {
            echo kubectl $@
          }

          gomplate() {
            echo gomplate $@
          }

          stackup() {
            echo stackup $@
          }

          xargs() {
            shift
            while read line; do
              echo $@ $line
            done
          }
        fi

        sub_${subcommand}

        if [ $? = 127 ]; then
            echo "Error: '$subcommand' is not a known subcommand." >&2
            echo "       Run '$progName --help' for a list of known subcommands." >&2
            exit 1
        fi
        ;;
esac
