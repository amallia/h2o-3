def call(buildConfig, stageConfig) {

  def defaultStage = load('h2o-3/scripts/jenkins/groovy/defaultStage.groovy')
  def stageNameToDirName = load('h2o-3/scripts/jenkins/groovy/stageNameToDirName.groovy')
  def s3upload = load('h2o-3/scripts/jenkins/groovy/s3upload.groovy')

  def DATASETS_FILE = 'accuracy_datasets_docker.csv'
  def TEST_CASES_FILE = "test_cases_${stageConfig.model}.csv"
  def BENCHMARK_RESULTS_FOLDER = 'benchmark_results'
  def BENCHMARK_SUMMARY_FILE = 'benchmark_results.csv'
  def ML_BENCHMARK_ROOT = "${env.WORKSPACE}/${stageNameToDirName(stageConfig.stageName)}/h2o-3/ml-benchmark"

  if (stageConfig.bucket == null) {
    stageConfig.bucket = 's3://test.0xdata.com'
  }
  if (stageConfig.datasetsPath == null) {
    stageConfig.datasetsPath = "${ML_BENCHMARK_ROOT}/h2oR/${DATASETS_FILE}"
  }
  if (stageConfig.testCasesPath == null) {
    stageConfig.testCasesPath = "${ML_BENCHMARK_ROOT}/h2oR/${TEST_CASES_FILE}"
  }
  if (stageConfig.benchmarkResultsRoot == null) {
    stageConfig.benchmarkResultsRoot = "${env.WORKSPACE}/${stageNameToDirName(stageConfig.stageName)}/${BENCHMARK_RESULTS_FOLDER}"
  }

  // Create folder for results and the benchmark_results.csv file, which will hold the summary for all datasets
  sh "mkdir -p ${stageConfig.benchmarkResultsRoot}"
  sh "touch ${stageConfig.benchmarkResultsRoot}/${BENCHMARK_SUMMARY_FILE}"

  if (stageConfig.archiveAdditionalFiles == null) {
    stageConfig.archiveAdditionalFiles = []
  }
  // by default the file path is prefixed with <STAGE_NAME>/h2o-3, therefore if we want to archive
  // <STAGE_NAME>/benchmark-results we have to add ../benchmark-results as additional file to archive
  stageConfig.archiveAdditionalFiles += ["../${BENCHMARK_RESULTS_FOLDER}/"]

  dir (ML_BENCHMARK_ROOT) {
    checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/mr/feature/size-benchmarking']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'c6bab81a-6bb5-4497-9ec9-285ef5db36ea', url: 'https://github.com/h2oai/ml-benchmark']]]
    sh "sed 's/s3:\\/\\/h2o-benchmark/\\/datasets/g' h2oR/accuracy_datasets_h2o.csv > h2oR/accuracy_datasets_docker.csv"
  }

  def benchmarkEnv = [
    "OUTPUT_PREFIX=${stageConfig.benchmarkResultsRoot}",
    "DATASETS_PATH=${stageConfig.datasetsPath}",
    "TEST_CASES_PATH=${stageConfig.testCasesPath}",
    "BENCHMARK_MODEL=${stageConfig.model}"
  ]
  withEnv(benchmarkEnv) {
    defaultStage(buildConfig, stageConfig)
  }

  s3upload(buildConfig.DEFAULT_IMAGE, buildConfig.DOCKER_REGISTRY, 5) {
    remoteArtifactBucket = stageConfig.bucket
    credentialsId = 'AWS S3 Credentials'
    updateLatest = false
    isRelease = false
    localArtifact = "${stageConfig.benchmarkResultsRoot}/${BENCHMARK_SUMMARY_FILE}"
    groupId = 'benchmarks'
    artifactId = stageNameToDirName(stageConfig.stageName)
    majorVersion = buildConfig.getMajorVersion()
    buildVersion = buildConfig.getBuildVersion()
  }
}

return this
