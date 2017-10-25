#!groovy

/**
This is a .groovy script for a parametrized Jenkins Pipeline Job that builds a CMake project.

The job expects a jenkins node with the name 'master'. The repository will be checked out on the master
and then copied to a slave-node that has the tag BuildSlaveTag. Then the job will call

$ cmake -H. -B_build <AdditionalGenerateArguments>
$ cmake --build _build <AdditionalBuildArguments>
    
in order to build the project. The job must provide the following paramters

RepositoryUrl                   - The url of the git repository the contains the projects CMakeLists.txt file in the root directory.
CheckoutDirectory               - A workspace directory on the master and build-slave into which the code is checked-out and which is used for the build.
BuildSlaveTag                   - The tag for the build-slave on which the project is build.
AdditionalGenerateArguments     - Arguments for the cmake generate call.
AdditionalBuildArguments        - Arguments for the cmake build call.   

*/


stage('Build')
{
    node(params.BuildSlaveTag)
    {
        // acquiering an extra workspace seems to be necessary to prevent interaction between
        // the parallel run nodes, although node() should already create an own workspace.
        ws(params.CheckoutDirectory)   
        {   
            // debug info
            printJobParameter()
        
            // checkout sources
            checkout([$class: 'GitSCM',
                userRemoteConfigs: [[url: params.RepositoryUrl]],
                branches: [[name: 'master']],
                extensions: [[$class: 'CleanBeforeCheckout']]]
            )
       
            // run cmake generate and build
            runCommand( 'cmake -E remove_directory _build')                             // make sure the build is clean
            runCommand( 'cmake -H. -B_build ' + params.AdditionalGenerateArguments )
            runCommand( 'cmake --build _build ' + params.AdditionalBuildArguments )
            
            echo '----- CMake project was build successfully -----'
        }
    }
}


def printJobParameter()
{
    def introduction = """
----- Build CMake project -----
RepositoryUrl = ${params.RepositoryUrl}
CheckoutDirectory = ${params.CheckoutDirectory}
BuildSlaveTag = ${params.BuildSlaveTag}
AdditionalGenerateArguments = ${params.AdditionalGenerateArguments}
AdditionalBuildArguments = ${params.AdditionalBuildArguments}
-------------------------------
"""
    
    echo introduction
}

def runCommand( command )
{
    if(isUnix())
    {
        sh command
    }
    else
    {
        bat command
    }
}
