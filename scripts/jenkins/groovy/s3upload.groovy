def call(image, registry, timeoutValue, body) {
    def insideDocker = load('h2o-3/scripts/jenkins/groovy/insideDocker.groovy')
    insideDocker([], image, registry, timeoutValue, 'MINUTES') {
        s3up body
    }
}

return this