#! /bin/bash

set -eo pipefail

DEBUG=""

progName=$(basename $0)
componentBuildPath="_all"
componentTemplatePath=""
env=""

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
    echo "    join      DEPRECATED: this is the same as compile now"
    echo "    deploy    Apply the compiled templates to a Kubernetes API server"
    echo "    delete    Delete the items in the compiled templates on a Kubernetes API server"
    echo ""
}

sub_clean() {
  rm -rf _build
}

sub_compile() {
  mkdir -p _build/$env/$componentBuildPath/templates
  gomplate --output-dir _build/$env/$componentBuildPath/templates --input-dir templates/$componentTemplatePath --datasource config=envs/$env.yaml
}

sub_validate() {
  sub_compile
  kubectl apply -R --validate --dry-run -f _build/$env/$componentBuildPath/templates/
}

sub_deploy() {
  sub_compile
  kubectl apply -R -f _build/$env/$componentBuildPath/templates/
}

sub_delete() {
  sub_compile
  kubectl delete -R -f _build/$env/$componentBuildPath/templates/
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
        fi

        sub_${subcommand}

        if [ $? = 127 ]; then
            echo "Error: '$subcommand' is not a known subcommand." >&2
            echo "       Run '$progName --help' for a list of known subcommands." >&2
            exit 1
        fi
        ;;
esac
