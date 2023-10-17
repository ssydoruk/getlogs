var HashMap = Java.type("java.util.HashMap");

var FIELDS = new HashMap();
const fs = require("fs");

var fileData = fs.readFileSync(__dirname + "/debug_data.txt").toString();

var recordFields = {
  name: "EventRouteRequest",
};

var RECORD = {
  getBytes() {
    return fileData;
  },
  getField(name) {
    return recordFields[name];
  },
  constToStr(constName, id) {
    var ret = "const " + constName + "(" + id + ")";
    return ret;
  },
};
module.exports = {
  FIELDS,
  RECORD,
};
