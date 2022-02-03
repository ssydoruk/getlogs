var cmd = RECORD.getField("command");
switch (cmd) {
  case "execution step":
  case "cond": {
    var s = RECORD.getBytes();
    var m;
    var expr;
    if ((m = s.match(/\(\):* (.+)/)) != undefined) {
      expr = m[1];
    }
    if (expr != undefined) {
   
        FIELDS.put("name", cmd + " ->" + expr);
      
    }
    break;
  }

  case "expression": {
    var s = RECORD.getBytes();
    var m;
    var expr = undefined;
    if ((m = s.match(/\(\):* (.+)/)) != undefined) {
      expr = m[1];
    }
    if (expr != undefined) {
      if (
        expr.includes("[__.savetmpfiles]") ||
        expr.includes("[__.savetmpfilesmode]")
      ) {
        IGNORE_RECORD = true;
      } else {
        FIELDS.put("name", cmd + " ->" + expr);
      }
    }

    break;
  }

  default:
    var param1 = RECORD.getField("param1");
    var param2 = RECORD.getField("param2");
    FIELDS.put(
      "name",
      cmd + (param1 ? " " + param1 : "") + (param2 ? " " + param2 : "")
    );
    break;
}
