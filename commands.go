package main

import (
	"fmt"
	"strings"
)

type Command []string

func (c Command) String() string {
	var a []string = c
	return strings.Join(a, " ")
}

func cmdCompile(env Environment, comp Component) []Command {
	compOutputDir := fmt.Sprintf("%s/%s/%s/templates", buildFolder, env, comp)
	compInputDir := fmt.Sprintf("templates/%s/", comp)

	if comp == "_all" {
		compInputDir = "templates/"
	}

	envFilePath := fmt.Sprintf("%s/%s.yaml", envFolder, env)

	return []Command{
		Command([]string{
			"mkdir",
			"-p",
			compOutputDir,
		}),
		Command([]string{
			compileCommand,
			"--output-dir", compOutputDir,
			"--input-dir", compInputDir,
			"--datasource", fmt.Sprintf("config=%s", envFilePath),
		}),
	}
}

func cmdValidate(env Environment, comp Component, files []FilePath) []Command {
	cmds := []Command{}

	for _, f := range files {
		cmds = append(cmds, Command(
			[]string{
				deployCommand,
				"apply",
				"--validate",
				"--dry-run",
				"-f",
				templateToBuildPath(env, comp, f),
			},
		))
	}

	return cmds
}

func cmdDeploy(env Environment, comp Component, files []FilePath) []Command {
	cmds := []Command{}

	for _, f := range filterCfn(files) {
		stackName := fmt.Sprintf("%s-%s", env, f)
		cmds = append(cmds, Command(
			[]string{
				cfnCommand,
				"up",
				stackName,
				"-t",
				templateToBuildPath(env, comp, f),
			},
		))
	}

	for _, f := range filterKubes(files) {
		cmds = append(cmds, Command(
			[]string{
				deployCommand,
				"apply",
				"-f",
				templateToBuildPath(env, comp, f),
			},
		))
	}

	return cmds
}

// func deploy(env Environment, comp Component, files []string) []Command {

// }

// func delete(env Environment, comp Component, files []string) []Command {

// }

func cmdClean() []Command {
	return []Command{
		Command([]string{
			"rm",
			"-rf",
			buildFolder,
		}),
	}
}
