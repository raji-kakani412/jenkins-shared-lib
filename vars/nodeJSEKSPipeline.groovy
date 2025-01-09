def call(Map configMap){
    pipeline {
        agent {
            label 'AGENT-1'
        }
        options{
            timeout(time:30, unit:'MINUTES')
            disableConcurrentBuilds()
            //retry(1)
        }
        parameters{
            booleanParam(name: 'deploy', defaultValue: false, description: 'Select to deploy or not')
        }
    
        environment {
            appVersion= ''
            region='us-east-1'
            account_id= ''
            project= configMap.get("project")
            environment='dev'
            component= configMap.get("component")
        }

        stages {
            stage('Read Version') {
                steps {
                    script{
                        def packageJson = readJSON file: 'package.json'
                        appVersion = packageJson.version
                        echo "App Version is ${appVersion}"
                    }

                }
            }
        
            stage('Install dependencies'){
                steps {
                sh  'npm install'
                }
            }
            /* stage('SonarQube Analysis'){
                environment{
                    SCANNER_HOME= tool 'sonar-6.0' // sonar scanner config
                }
                steps{
                    withSonarQubeEnv('sonar-6.0'){
                        sh '$SCANNER_HOME/bin/sonar-scanner'
                        // this is generic scanner, it automatically understands the language and provide scan results.
                    }
                }
            }
            stage("Quality Gate") {
                steps {
                    timeout(time: 5, unit: 'MINUTES') { // If analysis takes longer than indicated time, then build will be aborted
                        waitForQualityGate abortPipeline: true
                    }
                }
            }*/
            
            stage('Docker Build') {
                steps {
                    withAWS(region: 'us-east-1', credentials: 'aws-creds') {
                        sh """
                        aws ecr get-login-password --region ${region} | docker login --username AWS --password-stdin ${account_id}.dkr.ecr.us-east-1.amazonaws.com

                        docker build -t ${account_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${environment}/${component}:${appVersion} .

                        docker images

                        docker push ${account_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${environment}/${component}:${appVersion}
                        """
                    }
                }
            }
            stage('Deploy'){
                when{
                    expression {params.deploy}
                }
                steps{
                    build job: "../${component}-cd", parameters: [
                        string(name: 'version', value: "${appVersion}")
                        string(name: 'ENVIRONMENT', value:"${environment}")
                    ], wait: true
                }
            
        }
        post{
            always{
                sh 'echo This runs always'
                deleteDir()
            }
            success{
                sh 'echo This runs when pipeline is success'
            }
            failure{
                sh 'echo This runs when pipeline fails'
            }
        }
    }
}