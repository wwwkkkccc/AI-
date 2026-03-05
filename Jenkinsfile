// Jenkins 声明式流水线 —— AI 简历项目 CI/CD
pipeline {
  agent any  // 在任意可用的 Jenkins 节点上运行

  // ===== 流水线全局选项 =====
  options {
    timestamps()                                    // 控制台输出添加时间戳
    disableConcurrentBuilds()                       // 禁止同一流水线并发执行
    buildDiscarder(logRotator(numToKeepStr: "30"))  // 只保留最近 30 次构建记录
  }

  // ===== 参数化构建：可在 Jenkins 界面手动修改 =====
  parameters {
    string(name: "REPO_URL", defaultValue: "https://github.com/wwwkkkccc/AI-.git", description: "GitHub repository URL")       // Git 仓库地址
    string(name: "DEPLOY_BRANCH", defaultValue: "main", description: "Deploy branch")                                          // 部署分支
    string(name: "DEPLOY_HOST", defaultValue: "45.207.201.227", description: "Deploy server host")                              // 目标服务器 IP
    string(name: "DEPLOY_PORT", defaultValue: "22", description: "Deploy server SSH port")                                      // SSH 端口
    string(name: "DEPLOY_USER", defaultValue: "root", description: "Deploy server SSH user")                                    // SSH 登录用户
    string(name: "DEPLOY_PATH", defaultValue: "/opt/resume-ai-stack", description: "Deploy directory on server")                // 服务器上的部署目录
  }

  stages {
    // ===== 阶段一：拉取代码 =====
    stage("Checkout") {
      steps {
        // 从 GitHub 拉取指定分支的源码到 Jenkins 工作区
        checkout([
          $class: "GitSCM",
          branches: [[name: "*/${params.DEPLOY_BRANCH}"]],
          userRemoteConfigs: [[url: "${params.REPO_URL}"]]
        ])
      }
    }

    // ===== 阶段二：显示最新提交信息 =====
    stage("Show Commit") {
      steps {
        // 打印最近一次 Git 提交的哈希、作者、日期和消息
        sh "git --no-pager log -1 --pretty=format:'%h %an %ad %s' --date=iso"
      }
    }

    // ===== 阶段三：远程部署到服务器 =====
    stage("Deploy To Server") {
      steps {
        sh """
          set -euo pipefail
          chmod +x scripts/ci/deploy_remote.sh

          # 打包工作区源码，上传到远程服务器，通过 docker compose 构建并部署
          DEPLOY_HOST='${params.DEPLOY_HOST}' \
          DEPLOY_PORT='${params.DEPLOY_PORT}' \
          DEPLOY_USER='${params.DEPLOY_USER}' \
          DEPLOY_PATH='${params.DEPLOY_PATH}' \
          DEPLOY_BRANCH='${params.DEPLOY_BRANCH}' \
          PACKAGE_TAG='jenkins-${BUILD_NUMBER}' \
          SSH_KEY_PATH='/var/jenkins_home/.ssh/id_rsa' \
          ./scripts/ci/deploy_remote.sh
        """
      }
    }
  }

  // ===== 构建后操作 =====
  post {
    success {
      echo "Deploy success: ${params.DEPLOY_HOST} ${params.DEPLOY_PATH}"  // 部署成功提示
    }
    failure {
      echo "Deploy failed, check Jenkins console output"  // 部署失败提示
    }
  }
}
