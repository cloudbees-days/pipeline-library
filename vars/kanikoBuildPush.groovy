// vars/kanikoBuildPush.groovy
def call(String imageName, String imageTag = env.BUILD_NUMBER, String gcpProject = "core-workshop", String target = ".", String dockerFile="Dockerfile", Closure body) {
  def dockerReg = "gcr.io/${gcpProject}"
  imageName = "helloworld-nodejs"
  def label = "kaniko-${UUID.randomUUID().toString()}"
  def podYaml = libraryResource 'podtemplates/dockerBuildPush.yml'
  podTemplate(name: 'kaniko', label: label, yaml: podYaml, inheritFrom: 'default-jnlp', nodeSelector: 'type=agent') {
    node(label) {
      body()
      imageNameTag()
      gitShortCommit()
      def repoName = env.IMAGE_REPO.toLowerCase()
      container(name: 'kaniko', shell: '/busybox/sh') {
        withEnv(['PATH+EXTRA=/busybox:/kaniko']) {
          sh """#!/busybox/sh
            /kaniko/executor -f ${pwd()}/${dockerFile} -c ${pwd()} --build-arg context=${repoName} --build-arg buildNumber=${BUILD_NUMBER} --build-arg shortCommit=${env.SHORT_COMMIT} --build-arg commitAuthor=${env.COMMIT_AUTHOR} -d ${dockerReg}/helloworld-nodejs:${repoName}-${BUILD_NUMBER}
          """
        }
      }
      publishEvent event:jsonEvent("{'eventType':'containerImagePush', 'image':'${dockerReg}/helloworld-nodejs:${repoName}-${BUILD_NUMBER}'}"), verbose: true
    }
  }
}
