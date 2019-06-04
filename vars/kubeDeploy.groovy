// vars/kubeDeploy.groovy
def call(imageName, imageTag, githubCredentialId, repoOwner, hostnameURL) {
    def label = "kubectl"
    def podYaml = libraryResource 'podtemplates/kubeDeploy.yml'
    def deployYaml = libraryResource 'k8s/basicDeploy.yml'
    def repoName = env.IMAGE_REPO.toLowerCase()
    def envStagingRepo = "environment_staging"
    
    podTemplate(name: 'kubectl', label: label, yaml: podYaml) {
      node(label) {
        //create environment repo for prod if it doesn't already exist
        echo githubCredentialId
        withCredentials([usernamePassword(credentialsId: githubCredentialId, usernameVariable: 'USERNAME', passwordVariable: 'ACCESS_TOKEN')]) {
          echo repoOwner
          echo envStagingRepo
          //def repoNotExists = sh(script: '''
          //    curl -H "Authorization: token $ACCESS_TOKEN" https://api.github.com/repos/$repoOwner/$envStagingRepo | jq 'contains({message: "Not Found"})'
          //  ''', returnStdout: true)
         // echo repoNotExists
         // if(repoNotExists) {
         // sh(script: """
         //     curl -H "Authorization: token $ACCESS_TOKEN" --data '{"name":"${envStagingRepo}"}' https://api.github.com/orgs/${repoOwner}/repos
         //   """)
         // }
            //this eends mods
          //curl -H "Authorization: token ACCESS_TOKEN" --data '{"name":""}' https://api.github.com/orgs/ORGANISATION_NAME/repos
        }
        writeFile file: "deploy.yml", text: deployYaml
        sh("sed -i.bak 's#REPLACE_IMAGE_TAG#gcr.io/core-workshop/helloworld-nodejs:${repoName}-${BUILD_NUMBER}#' deploy.yml")
        sh("sed -i.bak 's#REPLACE_SERVICE_NAME#${repoName}#' deploy.yml")
        sh("sed -i.bak 's#REPLACE_HOSTNAMEURL#${hostnameURL}#' deploy.yml")  
        withCredentials([usernamePassword(credentialsId: githubCredentialId, usernameVariable: 'USERNAME', passwordVariable: 'ACCESS_TOKEN')]) {
          sh """
            git init
            git config user.email "deployBot@cb-sa.io"
            git config user.name "${USERNAME}"
            git add deploy.yml
            git commit -a -m 'updating ${envStagingRepo} deployment for ${repoName}'
            git remote add origin https://${USERNAME}:${ACCESS_TOKEN}@github.com/${repoOwner}/${envStagingRepo}.git
            git pull origin master
            git push -u origin master
          """
        }
        container("kubectl") {
          sh "kubectl apply -f deploy.yml"
            sh "echo 'deployed to ${hostnameURL}/${repoName}/'"
        }
      }
    }
}
