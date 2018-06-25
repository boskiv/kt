package main

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
)

type Environment string

type Validator interface {
	IsValid() bool
}

const (
	buildFolder    = "_build"
	envFolder      = "envs"
	templateFolder = "templates"
	compileCommand = "gomplate"
	deployCommand  = "kubectl"
	cfnCommand     = "stackup"
)

func getAllFiles() ([]FilePath, error) {
	files, filter := filterFiles()
	err := filepath.Walk(".", filter)

	if err != nil {
		return nil, err
	}

	return *files, nil
}

type commandRunner = func(Command) error

func runProgram(cmdStr Command) error {
	fmt.Println(cmdStr.String())
	cmd := exec.Command(cmdStr[0], cmdStr[1:]...)
	stdoutStderr, err := cmd.CombinedOutput()
	fmt.Printf("%s\n", stdoutStderr)
	if err != nil {
		fmt.Printf("%s\n", err)
		return err
	}
	return err
}

func logProgram(cmd Command) error {
	fmt.Println(cmd.String())
	return nil
}

func runCommands(cmds []Command, f commandRunner) error {
	for _, cmd := range cmds {
		err := f(cmd)
		if err != nil {
			return err
		}
	}
	return nil
}

func main() {
	if err := rootCmd.Execute(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}
