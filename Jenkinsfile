pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: "30"))
  }

  parameters {
    string(name: "REPO_URL", defaultValue: "https://gitee.com/wangknagchi/ai-resume.git", description: "Gitee repository URL")
    string(name: "DEPLOY_BRANCH", defaultValue: "main", description: "Deploy branch")
    string(name: "DEPLOY_HOST", defaultValue: "45.207.201.227", description: "Deploy server host")
    string(name: "DEPLOY_PORT", defaultValue: "22", description: "Deploy server SSH port")
    string(name: "DEPLOY_USER", defaultValue: "root", description: "Deploy server SSH user")
    string(name: "DEPLOY_PATH", defaultValue: "/opt/ai-resume", description: "Deploy directory on server")
  }

  environment {
    // SSH private key credential ID in Jenkins Credentials
    SSH_CREDENTIALS_ID = "resume-ai-server-ssh"
  }

  stages {
    stage("Checkout") {
      steps {
        // Checkout source from Gitee in Jenkins workspace
        checkout([
          $class: "GitSCM",
          branches: [[name: "*/${params.DEPLOY_BRANCH}"]],
          userRemoteConfigs: [[url: "${params.REPO_URL}"]]
        ])
      }
    }

    stage("Show Commit") {
      steps {
        sh "git --no-pager log -1 --pretty=format:'%h %an %ad %s' --date=iso"
      }
    }

    stage("Deploy To Server") {
      steps {
        sshagent(credentials: ["${env.SSH_CREDENTIALS_ID}"]) {
          sh """
            set -euo pipefail
            chmod +x scripts/ci/deploy_remote.sh

            # Package workspace, upload to server, then docker compose deploy remotely
            DEPLOY_HOST='${params.DEPLOY_HOST}' \
            DEPLOY_PORT='${params.DEPLOY_PORT}' \
            DEPLOY_USER='${params.DEPLOY_USER}' \
            DEPLOY_PATH='${params.DEPLOY_PATH}' \
            DEPLOY_BRANCH='${params.DEPLOY_BRANCH}' \
            PACKAGE_TAG='jenkins-${BUILD_NUMBER}' \
            ./scripts/ci/deploy_remote.sh
          """
        }
      }
    }
  }

  post {
    success {
      echo "Deploy success: ${params.DEPLOY_HOST} ${params.DEPLOY_PATH}"
    }
    failure {
      echo "Deploy failed, check Jenkins console output"
    }
  }
}
