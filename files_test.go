package main

import "testing"

func TestFilterKubes(t *testing.T) {
	files := []FilePath{
		"other/unrelated.txt",
		"templates/mycomp1/myfile1.yaml",
		"templates/mycomp1/myfile2.yaml",
		"templates/cfn/myfile2.yaml",
		"templates/mycomp2/myfile1.yaml",
	}

	actual := filterKubes(files)

	if len(actual) != 3 {
		t.Error(actual)
	}

	if actual[0] != "templates/mycomp1/myfile1.yaml" {
		t.Error(actual)
	}

	if actual[2] != "templates/mycomp2/myfile1.yaml" {
		t.Error(actual)
	}
}

func TestFilterCfn(t *testing.T) {
	files := []FilePath{
		"other/unrelated.txt",
		"templates/mycomp1/myfile1.yaml",
		"templates/mycomp1/myfile2.yaml",
		"templates/cfn/myfile2.yaml",
		"templates/mycomp2/myfile1.yaml",
	}

	actual := filterCfn(files)

	if len(actual) != 1 {
		t.Error(actual)
	}

	if actual[0] != "templates/cfn/myfile2.yaml" {
		t.Error(actual)
	}
}

func TestTemplateToBuildPathComponent(t *testing.T) {
	table := map[string]string{
		"templates/mycomp/file1.yaml":  "_build/qa/mycomp/templates/mycomp/file1.yaml",
		"templates/mycomp/file2.yaml":  "_build/qa/mycomp/templates/mycomp/file2.yaml",
		"templates/mycomp2/file1.yaml": "_build/qa/mycomp/templates/mycomp2/file1.yaml",
		"templates/xxx/file1.yaml":     "_build/qa/mycomp/templates/xxx/file1.yaml",
	}

	for file, expected := range table {
		converted := templateToBuildPath(Environment("qa"), NewComponent("mycomp"), file)

		if converted != expected {
			t.Error(converted)
		}
	}
}

func TestTemplateToBuildPathAll(t *testing.T) {
	converted := templateToBuildPath(Environment("qa"), NewComponent(""), "templates/test.yml")

	if converted != "_build/qa/_all/templates/test.yml" {
		t.Error(converted)
	}
}
