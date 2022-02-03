
// uncomment when debuging standalone
// const dbg=require('./DEBUG.js'); var RECORD=dbg.RECORD;  var FIELDS = dbg.FIELDS;

var m;

for (var s of RECORD.getBytes().split('\n')) {
  if ((m = s.match(/AttributePrivateMsgID\s*(\d*)$/i)) != undefined) {
    FIELDS.put("PrivateMsgID", "msgid=" + m[1]);
  } else if ((m = s.match(/AttributeLocation\s(\S*)$/i)) != undefined) {
    FIELDS.put("location", "loc=" + m[1]);
  } else if ((m = s.match(/(from ISCC):/)) != undefined) {
    FIELDS.put("source", m[1]);
  } else if ((m = s.match(/AttributeCallState\s(\d*)$/)) != undefined) {
    try {
      var i = parseInt(m[1]);
      if (
        i > 0 ||
        !RECORD.getField("name").toLowerCase().startsWith("request")
      ) {
        FIELDS.put("callParams", "state: " + RECORD.constToStr("CallState", i));
      }
    } catch (e) {}
  }
}
// console.log(FIELDS.entrySet());