#! /bin/bash

set -eo pipefail

DEBUG=""

progName=$(basename $0)
componentBuildPath="_all"
componentTemplatePath=""
env=""

cfnSubfolder="cfn"

parse_args() {
  while getopts ":e:dc:" opt; do
    case "${opt}" in
      e)
        if [[ "$OPTARG" == "" ]]; then
          echo "-e needs an environment" >&2
          exit 1
        fi

        env=$OPTARG
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
      *)
        echo "Invalid argument passed: -$opt" >&2
        exit 1
        ;;
    esac
  done
}

sub_help() {
    echo "Usage: $progName <subcommand> -e ENVIRONMENT [-c COMPONENT] [-d]"
    echo ""
    echo "Subcommands:"
    echo "    clean     Clean the compile folder (under _build)"
    echo "    compile   Use gomplate to compile the templates"
    echo "    validate  Validate the compiled templates against a Kubernetes API server"
    echo "    deploy    Apply the compiled templates to a Kubernetes API server"
    echo "    delete    Delete the items in the compiled templates on a Kubernetes API server"
    echo ""
}

sub_clean() {
  rm -rf _build
}

is_empty_file() {
  # File exists
  if [ -f $1 ]; then
    if [ -s $1 ]; then
      # Has content but with all whitespace
      grep -q '[^[:space:]]' < $1 && echo "False" || echo "True"
    else
      # File is empty
      echo "True"
    fi
  fi
}

sub_compile() {
  if [ -z "$env" ]; then
    echo "Must provide an environment!" >&2
    exit 1
  fi

  if [ ! -f "envs/$env.yaml" ]; then
    echo "The env file envs/$env.yaml does not exist!" >&2
    exit 2
  fi

  if [ ! -d "./templates/$componentTemplatePath" ]; then
    echo "The component directory templates/$componentTemplatePath does not exist!" >&2
    exit 2
  fi

  mkdir -p _build/$env/$componentBuildPath/templates
  gomplate \
    --output-dir _build/$env/$componentBuildPath/templates \
    --input-dir templates/$componentTemplatePath \
    --datasource config=envs/$env.yaml
}

sub_validate() {
  sub_compile

  # TODO: create a change-set in stackup for validation?

  find _build/$env/$componentBuildPath/templates/ -type f -name "*.yaml" \
    | grep -v "/$cfnSubfolder/" \
    | sort \
    | xargs -n1 kubectl apply --validate --dry-run -f
}

sub_deploy() {
  sub_compile

  for f in $(find _build/$env/$componentBuildPath/templates/ -type f -name "*.yaml" -path "*/$cfnSubfolder/*" | sort); do
    stackup $env-$(basename $f .yaml) up -t $f
  done

  for f in $(find _build/$env/$componentBuildPath/templates/ -type f -name "*.yaml" \
    | grep -v "/$cfnSubfolder/" \
    | sort); do
    if [ $(is_empty_file $f) != "True" ]; then
      kubectl apply -f $f
    fi
  done
}

sub_delete() {
  sub_compile

  find _build/$env/$componentBuildPath/templates/ -type f \
    | grep -v "/$CFN_SUBFOLDER/" \
    | sort -r \
    | xargs -n1 kubectl delete -f

  for f in $(find _build/$env/$componentBuildPath/templates/ -type f -path "*/$cfnSubfolder/*" -name "*.yaml" | sort); do
    stackup $env-$(basename $f .yaml) down
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

        echo "env: $env"
        echo "componentBuildPath: $componentBuildPath"
        echo "componentTemplatePath: $componentTemplatePath"
        echo "command: $subcommand"

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
