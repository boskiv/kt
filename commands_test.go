package main

import "testing"

func TestCompileCommandAll(t *testing.T) {
	cmds := cmdCompile(Environment("qa"), Component("_all"))

	if len(cmds) != 2 {
		t.Error(cmds)
	}

	if cmds[0].String() != "mkdir -p _build/qa/_all/templates" {
		t.Error(cmds[0])
	}

	if cmds[1].String() != "gomplate --output-dir _build/qa/_all/templates --input-dir templates/ --datasource config=envs/qa.yaml" {
		t.Error(cmds[1])
	}
}

func TestCompileCommandComponent(t *testing.T) {
	cmds := cmdCompile(Environment("qa1"), Component("mycomp"))

	if len(cmds) != 2 {
		t.Error(cmds)
	}

	if cmds[0].String() != "mkdir -p _build/qa1/mycomp/templates" {
		t.Error(cmds[0])
	}

	if cmds[1].String() != "gomplate --output-dir _build/qa1/mycomp/templates --input-dir templates/mycomp/ --datasource config=envs/qa1.yaml" {
		t.Error(cmds[1])
	}
}

func TestValidateCommandAll(t *testing.T) {
	files := []FilePath{
		"templates/mycomp1/myfile1.yaml",
		"templates/mycomp1/myfile2.yaml",
		"templates/mycomp2/myfile1.yaml",
	}

	cmds := cmdValidate(Environment("qa"), NewComponent(""), files)

	if len(cmds) != 3 {
		t.Error(cmds)
	}

	if cmds[0].String() != "kubectl apply --validate --dry-run -f _build/qa/_all/templates/mycomp1/myfile1.yaml" {
		t.Error(cmds[0])
	}

	if cmds[1].String() != "kubectl apply --validate --dry-run -f _build/qa/_all/templates/mycomp1/myfile2.yaml" {
		t.Error(cmds[1])
	}

	if cmds[2].String() != "kubectl apply --validate --dry-run -f _build/qa/_all/templates/mycomp2/myfile1.yaml" {
		t.Error(cmds[2])
	}
}

func TestDeployCommandAll(t *testing.T) {
	files := []FilePath{
		"templates/mycomp1/myfile1.yaml",
		"templates/mycomp1/myfile2.yaml",
		"templates/mycomp2/myfile1.yaml",
		"templates/cfn/template1.yaml",
		"templates/cfn/template2.yaml",
	}

	cmds := cmdDeploy(Environment("qa"), NewComponent(""), files)

	if len(cmds) != 5 {
		t.Error(cmds)
	}

	if cmds[0].String() != "stackup up qa-template1 -t templates/qa/_all/cfn/template1.yaml" {
		t.Error(cmds[0])
	}

	if cmds[1].String() != "stackup up qa-template2 -t templates/qa/_all/cfn/template2.yaml" {
		t.Error(cmds[1])
	}

	if cmds[2].String() != "kubectl apply -f _build/qa/_all/templates/mycomp1/myfile1.yaml" {
		t.Error(cmds[2])
	}

	if cmds[3].String() != "kubectl apply -f _build/qa/_all/templates/mycomp1/myfile2.yaml" {
		t.Error(cmds[3])
	}

	if cmds[4].String() != "kubectl apply -f _build/qa/_all/templates/mycomp2/myfile1.yaml" {
		t.Error(cmds[4])
	}
}

func TestCleanCommand(t *testing.T) {
	cmds := cmdClean()

	if len(cmds) != 1 {
		t.Error(cmds)
	}

	if cmds[0].String() != "rm -rf _build" {
		t.Error(cmds[0])
	}
}
