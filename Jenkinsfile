String cron_string = BRANCH_NAME == "develop" ? "0 1 * * *" : ""

pipeline {
  agent {
    docker {
      image 'maven:3.8.1-openjdk-11'
      args '-v /root/.m2:/root/.m2'
    }
  }
  parameters {
        choice(name: 'Env', choices: ['dev', 'acc'])
        booleanParam(name: 'SendReport', defaultValue: true, description: 'Send Report to XRay')
  }

  triggers { cron(cron_string) }
  stages {

    stage('test') {
        steps {
            sh 'mvn clean integration-test'
            script {
                if(params.SendReport) {
                    sh 'mvn exec:java -Dexec.mainClass="utils.SendReportToXRay"'
                }
            }

            archiveArtifacts("target/failsafe-reports/")
        }
    }
  }
}