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

import static Constants.*

class Constants {
    // stash names
    static final SOURCES_STASH = "SourcesStash"
}

// This node is the driver for the subjobs
node('master')
{
    def introduction = '''
----- Build CMake project -----
RepositoryUrl = ${params.RepositoryUrl}
CheckoutDirectory = ${params.CheckoutDirectory}
BuildSlaveTag = ${params.BuildSlaveTag}
AdditionalGenerateArguments = ${params.AdditionalGenerateArguments}
AdditionalBuildArguments = ${params.AdditionalBuildArguments}
'''
    
    println introduction
    
    addCheckoutSourcesStage()
    addBuildStage()
}

def addCheckoutSourcesStage()
{
    stage('Checkout')
    {
        ws(params.CheckoutDirectory)
        {
            // We do another clone/checkout because the checkout that is used to checkout this scipt ends up in a different directory. 
            // We also want to perform a clean checkout here.
            checkout([$class: 'GitSCM',
                userRemoteConfigs: [[url: params.RepositoryUrl]],
                branches: [[name: 'master']],
                extensions: [[$class: 'CleanBeforeCheckout']]]
            )

            // Stash source files for the build slaves
            stash includes: '**', name: SOURCES_STASH
        }
    }
}

def addBuildStage()
{
    stage('Build')
    {
        node(params.BuildSlaveTag)
        {
            // acquiering an extra workspace seems to be necessary to prevent interaction between
            // the parallel run nodes, although node() should already create an own workspace.
            ws(params.CheckoutDirectory)   
            {   
                // clean the workspace
                dir(params.CheckoutDirectory)
                {
                    deleteDir()
                }
                
                // Unstash the repository content
                unstash SOURCES_STASH
           
                runCommand( 'cmake -H. -B_build ' + params.AdditionalGenerateArguments )
                runCommand( 'cmake --build _build ' + params.AdditionalBuildArguments )
                
                echo '----- CMake project was build successfully -----'
            }
        }
    }
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
