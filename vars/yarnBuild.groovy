def call(String buildMode = "build", Closure body) {
  def label = "nodejs-${repoOwner}"
  def podYaml = libraryResource 'podtemplates/nodejs/pod.yml'
  podTemplate(name: 'nodejs', label: label, yaml: podYaml, podRetention: always(), idleMinutes: 30) {
    node(label) {
      body()
      if(env.BRANCH_NAME != "main") {
        buildMode = "build:dev" 
      }
      container('nodejs') {
        sh """
          yarn install
          yarn run $buildMode
        """
        stash name: "app", includes: "dist/**,.env*,nginx.conf,Dockerfile,version.txt" 
        stash name: "output", includes: "output/**" 
      }
    }
  }
}
