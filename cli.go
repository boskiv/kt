package main

import "github.com/spf13/cobra"

var component string

var rootCmd = &cobra.Command{
	Use:   "kt",
	Short: "Template and deploy kubernetes components.",
}

var cleanCmd = &cobra.Command{
	Use:   "clean",
	Short: "Clean the _build folder.",
	Run: func(cmd *cobra.Command, _args []string) {
		runCommands(cmdClean(), logProgram)
	},
}

var validateCmd = &cobra.Command{
	Use:   "validate",
	Short: "Compile and validate templates.",
	Args:  cobra.ExactArgs(1),
	Run: func(cmd *cobra.Command, args []string) {
		env := Environment(args[0])
		comp := NewComponent(component)
		files, err := getAllFiles()

		if err != nil {
			panic(err)
		}

		runCommands(cmdCompile(env, comp), logProgram)
		runCommands(cmdValidate(env, comp, filterKubes(files)), logProgram)
	},
}

var compileCmd = &cobra.Command{
	Use:   "compile",
	Short: "Compile templates using gomplate.",
	Args:  cobra.ExactArgs(1),
	Run: func(cmd *cobra.Command, args []string) {
		env := Environment(args[0])
		comp := NewComponent(component)
		runCommands(cmdCompile(env, comp), logProgram)
	},
}

func init() {
	compileCmd.Flags().StringVarP(&component, "component", "c", "", "The component to action (a folder under the templates folder).")
	rootCmd.AddCommand(cleanCmd, validateCmd, compileCmd)
}
