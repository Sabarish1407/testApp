// ==============================================================================
// Jenkinsfile for testApp (Customer / Order Pricing Service)
// Declarative Pipeline for Central Neuro Deploy Engine
// Full CI/CD: Build, Test, Docker Build, GHCR Container Push, Deploy, Git Push
// ==============================================================================

pipeline {
    agent any

    parameters {
        choice(name: 'ENVIRONMENT', choices: ['dev', 'demo', 'prod'], description: 'Deployment target environment')
        string(name: 'IMAGE_NAME', defaultValue: 'customer-service', description: 'Docker image repository name')
        string(name: 'IMAGE_TAG', defaultValue: 'latest', description: 'Docker image tag (e.g. 1.0.0-b9f2c81d)')
        string(name: 'DOCKERFILE_PATH', defaultValue: 'Dockerfile', description: 'Relative path to the Dockerfile')
        
        // Docker Build & Registry parameters
        booleanParam(name: 'BUILD_DOCKER_IMAGE', defaultValue: false, description: 'Build Docker container image (enable when Docker daemon is available)')
        booleanParam(name: 'PUSH_DOCKER_IMAGE', defaultValue: false, description: 'Push built Docker image to container registry (e.g. ghcr.io)')
        string(name: 'DOCKER_REGISTRY', defaultValue: 'ghcr.io', description: 'Container Registry host (e.g. ghcr.io, docker.io)')
        string(name: 'DOCKER_REGISTRY_OWNER', defaultValue: '', description: 'GitHub username or organization owning the container registry (e.g. your-github-user)')
        
        // Git Repository parameters (dynamically resolves from Git origin or parameter)
        booleanParam(name: 'AUTO_PUSH_GITHUB', defaultValue: false, description: 'Automatically push deployment updates and tags to Git repository')
        string(name: 'TARGET_REPO_URL', defaultValue: '', description: 'Target Git repository URL (leave blank to auto-detect from Git remote origin)')
        string(name: 'GITHUB_CREDENTIALS_ID', defaultValue: 'github-credentials', description: 'Jenkins Credentials ID for Git authentication (PAT with repo and write:packages scope)')
        string(name: 'TARGET_BRANCH', defaultValue: 'main', description: 'Target Git branch to push deployment updates')
        
        // GitHub Pull Request parameters
        booleanParam(name: 'CREATE_PULL_REQUEST', defaultValue: true, description: 'Automatically create a GitHub Pull Request from feature branch to target branch upon successful verification')
        string(name: 'SOURCE_BRANCH', defaultValue: '', description: 'Source feature branch for Pull Request (leave empty to auto-detect)')
        string(name: 'PR_TITLE', defaultValue: '', description: 'Pull Request title (leave empty for auto-generated title)')
        string(name: 'PR_BODY', defaultValue: '', description: 'Pull Request description body')
    }

    environment {
        APP_NAME = 'testApp'
        ENVIRONMENT = "${params.ENVIRONMENT ?: 'dev'}"
        IMAGE_NAME = "${params.IMAGE_NAME ?: 'customer-service'}"
        IMAGE_TAG = "${params.IMAGE_TAG ?: 'latest'}"
        
        // neuro-Deploy updates Dockerfile path here:
        DOCKERFILE_PATH = 'Dockerfile'
        
        // Docker Configuration
        BUILD_DOCKER_IMAGE = "${params.BUILD_DOCKER_IMAGE ?: false}"
        PUSH_DOCKER_IMAGE = "${params.PUSH_DOCKER_IMAGE ?: false}"
        DOCKER_REGISTRY = "${params.DOCKER_REGISTRY ?: 'ghcr.io'}"
        DOCKER_REGISTRY_OWNER = "${params.DOCKER_REGISTRY_OWNER ?: ''}"
        
        // Git Configuration
        AUTO_PUSH_GITHUB = "${params.AUTO_PUSH_GITHUB ?: false}"
        TARGET_REPO_URL = "${params.TARGET_REPO_URL ?: ''}"
        GITHUB_CREDENTIALS_ID = "${params.GITHUB_CREDENTIALS_ID ?: 'github-credentials'}"
        TARGET_BRANCH = "${params.TARGET_BRANCH ?: 'main'}"
        GIT_AUTHOR_NAME = 'Central Neuro Agent'
        GIT_AUTHOR_EMAIL = 'agent@central-neuro.io'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                script {
                    def workDir = (fileExists('pom.xml')) ? '.' : (fileExists('testApp/pom.xml') ? 'testApp' : '.')
                    dir(workDir) {
                        echo "Building application with Maven inside directory '${workDir}'..."
                        if (isUnix()) {
                            if (fileExists('mvnw')) {
                                sh 'chmod +x mvnw && ./mvnw clean install'
                            } else {
                                sh 'mvn clean install'
                            }
                        } else {
                            if (fileExists('mvnw.cmd')) {
                                bat 'call mvnw.cmd clean install'
                            } else {
                                bat 'mvn clean install'
                            }
                        }
                    }
                }
            }
        }

        stage('Publish Test Results') {
            steps {
                junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
            }
        }

        stage('Docker Build') {
            when {
                expression { return env.BUILD_DOCKER_IMAGE == 'true' || params.BUILD_DOCKER_IMAGE == true }
            }
            steps {
                script {
                    def workDir = (fileExists('pom.xml')) ? '.' : (fileExists('testApp/pom.xml') ? 'testApp' : '.')
                    dir(workDir) {
                        echo "Building local Docker image ${env.IMAGE_NAME}:${env.IMAGE_TAG} using Dockerfile at '${env.DOCKERFILE_PATH}'..."
                        def buildCmd = "docker build -t ${env.IMAGE_NAME}:${env.IMAGE_TAG} -f ${env.DOCKERFILE_PATH} ."
                        if (isUnix()) {
                            sh buildCmd
                        } else {
                            bat buildCmd
                        }
                    }
                }
            }
        }

        stage('Push Docker Image to Registry') {
            when {
                expression { return env.PUSH_DOCKER_IMAGE == 'true' || params.PUSH_DOCKER_IMAGE == true }
            }
            steps {
                script {
                    def registryOwner = (env.DOCKER_REGISTRY_OWNER ?: '').trim().toLowerCase()
                    def repoImage = env.IMAGE_NAME.toLowerCase()
                    
                    // Format image name based on whether owner is provided
                    def imageTarget = registryOwner ? "${registryOwner}/${repoImage}" : repoImage
                    def fullImageTag = "${env.DOCKER_REGISTRY}/${imageTarget}:${env.IMAGE_TAG}"
                    def envLatestTag = "${env.DOCKER_REGISTRY}/${imageTarget}:${env.ENVIRONMENT}-latest"

                    echo "Tagging Docker images for Registry (${env.DOCKER_REGISTRY}):"
                    echo " -> ${fullImageTag}"
                    echo " -> ${envLatestTag}"

                    if (isUnix()) {
                        sh "docker tag ${env.IMAGE_NAME}:${env.IMAGE_TAG} ${fullImageTag}"
                        sh "docker tag ${env.IMAGE_NAME}:${env.IMAGE_TAG} ${envLatestTag}"
                    } else {
                        bat "docker tag ${env.IMAGE_NAME}:${env.IMAGE_TAG} ${fullImageTag}"
                        bat "docker tag ${env.IMAGE_NAME}:${env.IMAGE_TAG} ${envLatestTag}"
                    }

                    echo "Authenticating and pushing image to ${env.DOCKER_REGISTRY}..."
                    try {
                        withCredentials([usernamePassword(
                            credentialsId: env.GITHUB_CREDENTIALS_ID,
                            usernameVariable: 'REGISTRY_USER',
                            passwordVariable: 'REGISTRY_TOKEN'
                        )]) {
                            if (isUnix()) {
                                sh "echo \"\$REGISTRY_TOKEN\" | docker login ${env.DOCKER_REGISTRY} -u \"\$REGISTRY_USER\" --password-stdin"
                                sh "docker push ${fullImageTag}"
                                sh "docker push ${envLatestTag}"
                                sh "docker logout ${env.DOCKER_REGISTRY}"
                            } else {
                                bat "echo %REGISTRY_TOKEN% | docker login ${env.DOCKER_REGISTRY} -u %REGISTRY_USER% --password-stdin"
                                bat "docker push ${fullImageTag}"
                                bat "docker push ${envLatestTag}"
                                bat "docker logout ${env.DOCKER_REGISTRY}"
                            }
                            echo "Successfully pushed Docker images to ${env.DOCKER_REGISTRY}!"
                        }
                    } catch (Exception dockerPushEx) {
                        echo "Warning: Pushing Docker image to ${env.DOCKER_REGISTRY} encountered an issue: ${dockerPushEx.getMessage()}"
                        echo "Ensure your registry credentials (e.g. GitHub PAT with 'write:packages') are configured in '${env.GITHUB_CREDENTIALS_ID}'."
                    }
                }
            }
        }

        stage('Deploy to Target Environment') {
            steps {
                script {
                    echo "Deploying to target environment: ${env.ENVIRONMENT}"
                    if (env.ENVIRONMENT == 'dev' || env.ENVIRONMENT == 'demo') {
                        echo "Applying deployment configuration for ${env.ENVIRONMENT}..."
                        if (env.BUILD_DOCKER_IMAGE == 'true' || params.BUILD_DOCKER_IMAGE == true) {
                            def deployCmd = "docker tag ${env.IMAGE_NAME}:${env.IMAGE_TAG} ${env.IMAGE_NAME}:${env.ENVIRONMENT}-latest"
                            if (isUnix()) {
                                sh deployCmd
                            } else {
                                bat deployCmd
                            }
                            echo "Successfully deployed Docker image ${env.IMAGE_NAME}:${env.IMAGE_TAG} to ${env.ENVIRONMENT}."
                        } else {
                            echo "Environment manifest and test verification completed for ${env.ENVIRONMENT}."
                        }
                    } else {
                        error("Direct deployment to '${env.ENVIRONMENT}' is rejected. Production deployment requires human gate approval.")
                    }
                }
            }
        }

        stage('Push to Git Repository') {
            when {
                expression { return env.AUTO_PUSH_GITHUB == 'true' || params.AUTO_PUSH_GITHUB == true }
            }
            steps {
                script {
                    def workDir = (fileExists('pom.xml')) ? '.' : (fileExists('testApp/pom.xml') ? 'testApp' : '.')
                    dir(workDir) {
                        // 1. Detect target Git remote URL dynamically
                        def remoteUrl = env.TARGET_REPO_URL ? env.TARGET_REPO_URL.trim() : ""
                        if (!remoteUrl) {
                            try {
                                if (isUnix()) {
                                    remoteUrl = sh(returnStdout: true, script: "git config --get remote.origin.url").trim()
                                } else {
                                    remoteUrl = bat(returnStdout: true, script: "@git config --get remote.origin.url").trim()
                                }
                            } catch (Exception ignored) {}
                        }

                        echo "Target Git remote URL resolved to: ${remoteUrl ?: 'origin'}"

                        // 2. Configure local Git identity
                        if (isUnix()) {
                            sh """
                                git config user.name "${env.GIT_AUTHOR_NAME}"
                                git config user.email "${env.GIT_AUTHOR_EMAIL}"
                            """
                        } else {
                            bat """
                                git config user.name "${env.GIT_AUTHOR_NAME}"
                                git config user.email "${env.GIT_AUTHOR_EMAIL}"
                            """
                        }

                        // 3. Stage and commit deployment changes (e.g. updated Dockerfile, env yaml)
                        def commitMsg = "chore(deploy): automated deploy update for ${env.IMAGE_NAME}:${env.IMAGE_TAG} [${env.ENVIRONMENT}] [skip ci]"
                        if (isUnix()) {
                            sh """
                                git add -A
                                git diff-index --quiet HEAD || git commit -m "${commitMsg}"
                            """
                        } else {
                            bat """
                                git add -A
                                git diff-index --quiet HEAD || git commit -m "${commitMsg}"
                            """
                        }

                        // 4. Tag the deployment release
                        def releaseTag = "deploy-${env.ENVIRONMENT}-${env.IMAGE_TAG}".replaceAll("[^a-zA-Z0-9._-]", "-")
                        echo "Creating deployment release tag: ${releaseTag}"
                        if (isUnix()) {
                            sh "git tag -f -a \"${releaseTag}\" -m \"Automated deployment release ${env.IMAGE_NAME}:${env.IMAGE_TAG} [${env.ENVIRONMENT}]\""
                        } else {
                            bat "git tag -f -a \"${releaseTag}\" -m \"Automated deployment release ${env.IMAGE_NAME}:${env.IMAGE_TAG} [${env.ENVIRONMENT}]\""
                        }

                        // 5. Push branch and tag using Jenkins credentials
                        try {
                            withCredentials([usernamePassword(
                                credentialsId: env.GITHUB_CREDENTIALS_ID,
                                usernameVariable: 'GH_USER',
                                passwordVariable: 'GH_TOKEN'
                            )]) {
                                echo "Authenticated via Jenkins Credentials ID '${env.GITHUB_CREDENTIALS_ID}'"
                                def pushTarget = "origin"
                                if (remoteUrl && remoteUrl.startsWith("https://")) {
                                    pushTarget = remoteUrl.replace("https://", "https://${GH_USER}:${GH_TOKEN}@")
                                }
                                
                                if (isUnix()) {
                                    sh """
                                        git push "${pushTarget}" HEAD:${env.TARGET_BRANCH}
                                        git push "${pushTarget}" "${releaseTag}" --force
                                    """
                                } else {
                                    bat """
                                        git push "${pushTarget}" HEAD:${env.TARGET_BRANCH}
                                        git push "${pushTarget}" "${releaseTag}" --force
                                    """
                                }
                                echo "Successfully pushed branch '${env.TARGET_BRANCH}' and tag '${releaseTag}'!"
                            }
                        } catch (Exception credError) {
                            echo "Warning: Git push encountered an issue: ${credError.getMessage()}"
                            echo "Ensure Jenkins has a credential with ID '${env.GITHUB_CREDENTIALS_ID}' configured."
                        }
                    }
                }
            }
        }

        stage('Create GitHub Pull Request') {
            when {
                expression { 
                    return env.CREATE_PULL_REQUEST == 'true' || params.CREATE_PULL_REQUEST == true 
                }
            }
            steps {
                script {
                    def workDir = (fileExists('pom.xml')) ? '.' : (fileExists('testApp/pom.xml') ? 'testApp' : '.')
                    dir(workDir) {
                        // 1. Detect branch name
                        def srcBranch = (params.SOURCE_BRANCH ?: env.BRANCH_NAME ?: env.GIT_BRANCH ?: '').trim()
                        if (!srcBranch) {
                            try {
                                if (isUnix()) {
                                    srcBranch = sh(returnStdout: true, script: "git rev-parse --abbrev-ref HEAD").trim()
                                } else {
                                    srcBranch = bat(returnStdout: true, script: "@git rev-parse --abbrev-ref HEAD").trim()
                                }
                            } catch (Exception ignored) {}
                        }
                        if (srcBranch.startsWith('origin/')) {
                            srcBranch = srcBranch.substring(7)
                        }

                        echo "Detected source branch: '${srcBranch}'"

                        // Skip Pull Request creation if already on target branch (e.g. main)
                        if (srcBranch == env.TARGET_BRANCH || srcBranch == 'main' || !srcBranch) {
                            echo "Pull Request not required: branch '${srcBranch}' matches target branch '${env.TARGET_BRANCH}'."
                            return
                        }

                        // 2. Resolve GitHub repository owner and name from remote origin URL
                        def remoteUrl = (env.TARGET_REPO_URL ?: '').trim()
                        if (!remoteUrl) {
                            try {
                                if (isUnix()) {
                                    remoteUrl = sh(returnStdout: true, script: "git config --get remote.origin.url").trim()
                                } else {
                                    remoteUrl = bat(returnStdout: true, script: "@git config --get remote.origin.url").trim()
                                }
                            } catch (Exception ignored) {}
                        }

                        echo "Target remote URL: ${remoteUrl}"
                        def repoMatcher = (remoteUrl =~ /(?:github\.com[\/:])([^\/]+)\/([^\/\.]+)/)
                        if (!repoMatcher.find()) {
                            echo "Warning: Could not parse GitHub owner/repo from remote URL '${remoteUrl}'."
                            return
                        }
                        def ghOwner = repoMatcher.group(1)
                        def ghRepo = repoMatcher.group(2)
                        echo "GitHub Target Repository: ${ghOwner}/${ghRepo}"

                        // 3. Prepare PR metadata
                        def defaultPrTitle = "feat(${srcBranch}): automated verified changes by Central Neuro"
                        def prTitle = (params.PR_TITLE ?: defaultPrTitle).trim()
                        def prBody = (params.PR_BODY ?: """## Central Neuro Automated PR
### Verification & Pipeline Summary:
- **Source Branch:** `${srcBranch}`
- **Target Branch:** `${env.TARGET_BRANCH}`
- **Build Status:** Verified (Unit & Integration tests passed via Maven)
- **Image:** `${env.IMAGE_NAME}:${env.IMAGE_TAG}`
- **Triggered By:** Central Neuro CI/CD Workflow
""").trim()

                        // 4. Open Pull Request via GitHub REST API
                        try {
                            withCredentials([usernamePassword(
                                credentialsId: env.GITHUB_CREDENTIALS_ID,
                                usernameVariable: 'GH_USER',
                                passwordVariable: 'GH_TOKEN'
                            )]) {
                                def apiUrl = "https://api.github.com/repos/${ghOwner}/${ghRepo}/pulls"
                                echo "Creating Pull Request via GitHub API: ${apiUrl}"
                                
                                def jsonPayload = groovy.json.JsonOutput.toJson([
                                    title: prTitle,
                                    head: srcBranch,
                                    base: env.TARGET_BRANCH,
                                    body: prBody
                                ])

                                writeFile file: 'pr_payload.json', text: jsonPayload

                                def curlCmd = """curl -s -X POST "${apiUrl}" -H "Accept: application/vnd.github+json" -H "Authorization: Bearer ${GH_TOKEN}" -H "X-GitHub-Api-Version: 2022-11-28" -d @pr_payload.json"""
                                
                                def response = ""
                                if (isUnix()) {
                                    response = sh(returnStdout: true, script: curlCmd).trim()
                                    sh 'rm -f pr_payload.json'
                                } else {
                                    response = bat(returnStdout: true, script: "@" + curlCmd).trim()
                                    bat '@del /f /q pr_payload.json 2>nul'
                                }

                                if (response.contains('"html_url"')) {
                                    def urlMatcher = (response =~ /"html_url":\s*"([^"]+)"/)
                                    if (urlMatcher.find()) {
                                        echo "=========================================================="
                                        echo ">>> GitHub Pull Request successfully created:"
                                        echo ">>> ${urlMatcher.group(1)}"
                                        echo "=========================================================="
                                    } else {
                                        echo "Pull Request created successfully! Response: ${response}"
                                    }
                                } else if (response.contains('A pull request already exists')) {
                                    echo "A Pull Request for branch '${srcBranch}' already exists on GitHub."
                                } else {
                                    echo "GitHub API response: ${response}"
                                }
                            }
                        } catch (Exception prEx) {
                            echo "Note during PR creation: ${prEx.getMessage()}"
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs deleteDirs: true, notFailBuild: true
        }
        success {
            echo "Jenkins deployment job succeeded for ${env.IMAGE_NAME}:${env.IMAGE_TAG} in ${env.ENVIRONMENT}."
        }
        failure {
            echo "Jenkins deployment job failed for ${env.IMAGE_NAME}:${env.IMAGE_TAG} in ${env.ENVIRONMENT}."
        }
    }
}
