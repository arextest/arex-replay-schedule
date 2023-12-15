var ArexAssertion = function (caseId) {
  this.caseId = caseId;
  this.equals = function (category, operation, path, originalText, expected, actual) {
    var result = expected === actual;
    var message = result ? "ok" : "failed, Expected: " + expected + " but was: " + actual;
    this.reportData(category, operation, path, originalText, message, result);
  };
  this.reportData = function (category, operation, path, originalText, message, result) {
    expectationService.save(this.caseId, category, operation, path, message, originalText, result);
  }
};
