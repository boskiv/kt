[![Build Status](https://travis-ci.org/MYOB-Technology/kt.svg?branch=master)](https://travis-ci.org/MYOB-Technology/kt)

# kt

This repo provides a docker image as a tool to use `gomplate` with `kubectl` and `stackup` to deploy Kubernetes apps and AWS Cloudformation templates across different environments with templating.

## Usage

To use this tool, add it to your `docker-compose.yml` file as the following service:

```yaml
services:
  kt:
    image: myobplatform/kt
    volumes:
      - "$HOME/.kube:/root/.kube"
      - "$HOME/.aws:/root/.aws"
      - ".:/app"
    environment:
      AWS_REGION: <your AWS Region>
```

You can now run the tool as required with `docker-compose run --rm kt <command>` where `<command>` is one of the following:

* *validate*: Compiles the templates with a given env (given via the `-e ENV` flag) and will validate the validity of the compiles manifests againsts the Kubernetes API server.
* *deploy*: Will compile and deploy the manifests files for an environment (given via the `-e ENV` flag). If there is also a `cfn` folder it will stackup the Cloudformation template *before* it deploys the Kubernetes manifests.
* *delete*: **CAUTION**, will compile and join the manifests and then delete all the Objects on the API server that are named in the compiled manifests. If there is a `cfn` folder it will also delete the Cloudformation stack *after* it deletes Kubernetes objects.

*Note: Currently `kt` will only treat files as being a Cloudformation or Kubernetes manifest file if they use the `.yaml` extension, NOT `.yml`. This is just to enforce consistenty.*

### Command line flags

`kt` has two flags to use when running the commands above:

* -e ENVIRONMENT  The Kubernetes environment to deploy to (name of file in 'env' folder sans .yaml).
* -d Provides a 'dry run' mode to see what commands WOULD have been executed.
* -c COMPONENT  The component (a subfolder under your templates dir) you want to deploy. You can group components inside folders and go arbitrarily deep. To deploy a component inside a group specify the path eg group/component1.

## Conventions

The `kt` tool assumes the following conventions of your project:

* You put your `gomplate` files in the `templates` folder. You can create sub folders under that to arbitrary depth.
* Inside the `templates` folder you group components in their own subfolders. Each component subfolder may have a `cfn` folder as well which would contain an AWS Cloudformation template.
* You put the environment files, AKA the `gomplate` datasource files in the `envs` folder and name each file after the environment.

## Templating

The templating available to you for files in the `templates` folder is using the gomplate cli tool, so visit [their docs](https://gomplate.hairyhenderson.ca/syntax/) for a list of templating functions available to you.

On top of the gomplate functions, `kt` adds in the following additional power that is specific to creating Kubernetes manifest templates:

* Reference `datasource "config"` in a template to pull out the values that you specify in your `envs` files, eg `{{ $config := (datasource "config") }}` will give you a variable that you can access for env values using dot notation such as `{{ $config.envValue }}` where `envValue` is the YAML key in each of your `envs` files.
* You are able to access all other template contents from a template using `gomplate`'s `file.Read` function. just specify the relative path from the root folder of the project as the relative path, eg `{{ file.Read "templates/mycomponent/config-map.yaml" }}` to access the contents of the `templates/mycomponent/config-map.yaml` file. _NOTE: To avoid circular dependencies and other issues, note that including any other template will be done without any template compilation - it is the raw file from disk._

### Ordering

The template files are joined in alphabetical order. This means that one can control the order in which objects are applied to the Kubernetes API server by simply prefixing files with numbers to force the ordering.

## Deploying

Any files in a component's folder or any subfolder NOT named `cfn` will be applied to the Kubernetes API server after being compiled with gomplate. If there is also a `cfn` folder that will be run with `stackup` as a AWS Cloudformation stack.

The naming convention for the Cloudformation stack will be `<env>-<cfn template filename>`. So if there is a file called `templates/component/cfn/backup-iam-role.yaml` and you run `kt` with `-e cluster01-test` the Cloudformation stack name will be `cluster01-test-backup-iam-role`.

## Helper scripts

`kt` comes with some extra scripts to help do odd jobs with Kubernetes. See the [./scripts](./scripts) folder for what they are and what they do. All these scripts are available on the `$PATH` inside the kt docker image so can be accessed easily by overriding the `entrypoint` when needed, eg `docker-compose run --entrypoint k-forward kt <extra args>`.

## Development

`kt` is simply a combination of the following tools with folder conventions:

* [gomplate](https://gomplate.hairyhenderson.ca/) to provide templating of any files in the `templates` directory to allow for different environments and complex setup.
* [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/) to do the actual deployment of the compiled template manifests.
* [stackup](https://github.com/realestate-com-au/stackup) to idempotently and synchronously deploy AWS Cloudformation stacks.

By using pre made tools such as gomplate we get alot of useful extras already built in, such as extra functions for templating.
