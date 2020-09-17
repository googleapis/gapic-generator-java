def _junit_output_impl(ctx):
    test_class_name = ctx.attr.test_class_name
    arguments = [test_class_name]
    inputs = ctx.files.srcs
    output = ctx.outputs.output
    test_runner = ctx.executable.test_runner

    command = """
    mkdir local_tmp  
    TEST_CLI_HOME="$(pwd)/local_tmp" \
    {test_runner_path} $@ &&
    cd local_tmp 
    zip -r ../{output} .
        """.format(
            test_runner_path = test_runner.path,
            output=output.path,
        )
    ctx.actions.run_shell(
        inputs = inputs,
        outputs = [output],
        arguments = arguments,
        tools = [test_runner],
        command = command,
    )

junit_output_zip = rule(
    attrs = {
        "test_class_name": attr.string(mandatory=True),
        "srcs": attr.label_list(
            allow_files = True,
            mandatory = True,
        ),
        "test_runner": attr.label(
            mandatory = True,
            executable = True,
            cfg = "host",
        ),
        "output_suffix": attr.string(mandatory = False, default = ".zip"),
    },
    outputs = {
        "output": "%{name}%{output_suffix}",
    },
    implementation = _junit_output_impl,  
)
    
def _overwrite_golden_impl(ctx):
    script_content = """\
    #!/bin/bash
    cd ${{BUILD_WORKSPACE_DIRECTORY}}
    echo {unit_test_results}
    unzip -ao {unit_test_results} -d src/test/java
    """.format(
        unit_test_results = ctx.file.unit_test_results.path,
    )
    ctx.actions.write(
        output = ctx.outputs.bin,
        content = script_content,
        is_executable = True,
    )
    return [DefaultInfo(executable = ctx.outputs.bin)]


overwrite_golden = rule(
    attrs = {
        "unit_test_results": attr.label(
            mandatory = False,
            allow_single_file = True),
    },
    outputs = {
        "bin": "%{name}.sh",
    },
    executable = True,
    implementation = _overwrite_golden_impl,
)

def update_golden(name, test_class_name, test_runner, srcs):
    jnuit_output_name = "%s_output" % name
    junit_output_zip(
        name = jnuit_output_name,
        test_class_name = test_class_name,
        test_runner = test_runner,
        srcs = srcs,
    )
    overwrite_golden(
        name = name,
        unit_test_results = ":%s" % jnuit_output_name
    )
