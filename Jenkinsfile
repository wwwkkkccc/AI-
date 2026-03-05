pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: "30"))
  }

  parameters {
    string(name: "REPO_URL", defaultValue: "https://gitee.com/wangknagchi/ai-resume.git", description: "Gitee 仓库地址")
    string(name: "DEPLOY_BRANCH", defaultValue: "main", description: "部署分支")
    string(name: "DEPLOY_HOST", defaultValue: "45.207.201.227", description: "部署服务器 IP / 域名")
    string(name: "DEPLOY_PORT", defaultValue: "22", description: "部署服务器 SSH 端口")
    string(name: "DEPLOY_USER", defaultValue: "root", description: "部署服务器 SSH 用户")
    string(name: "DEPLOY_PATH", defaultValue: "/opt/ai-resume", description: "服务器代码目录")
  }

  environment {
    // 需要在 Jenkins -> Credentials 中创建同名 SSH 私钥凭据
    SSH_CREDENTIALS_ID = "resume-ai-server-ssh"
  }

  stages {
    stage("Checkout") {
      steps {
        // 每次构建都从 Gitee 拉取指定分支，确保构建上下文可复现
        checkout([
          $class: "GitSCM",
          branches: [[name: "*/${params.DEPLOY_BRANCH}"]],
          userRemoteConfigs: [[url: "${params.REPO_URL}"]]
        ])
      }
    }

    stage("Show Commit") {
      steps {
        sh """
          git --no-pager log -1 --pretty=format:'%h %an %ad %s' --date=iso
        """
      }
    }

    stage("Deploy To Server") {
      steps {
        // 使用 SSH Agent 插件加载服务器私钥，执行远端拉取和 docker compose 重建
        sshagent(credentials: ["${env.SSH_CREDENTIALS_ID}"]) {
          sh """
            ssh -o StrictHostKeyChecking=no -p ${params.DEPLOY_PORT} ${params.DEPLOY_USER}@${params.DEPLOY_HOST} <<'EOF'
            set -euo pipefail

            DEPLOY_PATH='${params.DEPLOY_PATH}'
            DEPLOY_BRANCH='${params.DEPLOY_BRANCH}'
            REPO_URL='${params.REPO_URL}'

            if [ ! -d "\${DEPLOY_PATH}/.git" ]; then
              mkdir -p "\${DEPLOY_PATH}"
              rm -rf "\${DEPLOY_PATH}"
              git clone "\${REPO_URL}" "\${DEPLOY_PATH}"
            fi

            cd "\${DEPLOY_PATH}"
            git fetch --all --prune
            git checkout "\${DEPLOY_BRANCH}"
            git reset --hard "origin/\${DEPLOY_BRANCH}"

            docker compose up -d --build backend frontend
            docker compose ps
            EOF
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
