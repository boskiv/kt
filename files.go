package main

import (
	"fmt"
	"os"
	"path/filepath"
	"regexp"
)

type FilePath = string

type Component string

func NewComponent(s string) Component {
	comp := Component(s)

	if s == "" {
		comp = Component("_all")
	}

	return comp
}

func filterFiles() (*[]FilePath, filepath.WalkFunc) {
	fl := &[]FilePath{}

	return fl, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		git, err := regexp.MatchString("^.*.git/.*$", path)

		if err != nil {
			return err
		}

		if git {
			return nil
		}

		if info.IsDir() {
			return nil
		}

		*fl = append(*fl, path)

		return nil
	}
}

func filterCfn(files []FilePath) []FilePath {
	filtered := []string{}

	for _, f := range files {
		shouldShow, err := regexp.MatchString("^templates/cfn/.*.(yaml|yml)$", f)

		if err != nil {
			panic(err)
		}

		if shouldShow {
			filtered = append(filtered, f)
		}
	}

	return filtered
}

func filterKubes(files []FilePath) []FilePath {
	filtered := []string{}

	for _, f := range files {
		isTemplate, err := regexp.MatchString("^templates/.*.(yaml|yml)$", f)
		if err != nil {
			panic(err)
		}

		isCfn, err := regexp.MatchString("^templates/cfn/.*.(yaml|yml)$", f)
		if err != nil {
			panic(err)
		}

		if isTemplate && !isCfn {
			filtered = append(filtered, f)
		}
	}

	return filtered
}

func templateToBuildPath(env Environment, comp Component, templateFile FilePath) FilePath {
	return fmt.Sprintf("%s/%s/%s/%s", buildFolder, env, comp, templateFile)
}
