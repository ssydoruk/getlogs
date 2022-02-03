switch (RECORD.getField("metric")) {
    case "eval_expr": {
      var s = RECORD.getBytes();
      var m;
      var expr;
      if ((m = s.match(/expression='([^\']+)' /)) != undefined) {
        expr = m[1];
      }
      if (expr != undefined) {
        if (
          expr.startsWith("storeEvent") ||
          expr.startsWith("storeUnhandledEvent") 
        ) {
            IGNORE_RECORD = true;
        } else {
            var type;
            var result;
  
            if ((m = s.match(/type=[']([^\']+)' /)) != undefined) {
                type= m[1];
            }
            if ((m = s.match(/result='([^\']+)' /)) != undefined) {
                result=m[1];
            }
            if(result != undefined && result.endsWith('.script.done') ){
                IGNORE_RECORD = true;
            }
            else {
                FIELDS.put("param2", "result:" + result);
                FIELDS.put("param1", "type:" + type);
                FIELDS.put("param", expr);
            }
        }
      }
      break;
    }
  
    case "eval_condition": {
      var s = RECORD.getBytes();
      var m;
      if ((m = s.match(/condition='(.+)' result='([^\']+)' /)) != undefined) {
        FIELDS.put("param", m[1]);
        FIELDS.put("param1", "result:" + m[2]);
      }
      break;
    }
  
    case "transition": {
      var s = RECORD.getBytes();
      var m;
      if ((m = s.match(/name='([^\']+)' event='([^\']+)'/)) != undefined) {
        FIELDS.put("param", m[1]);
        var p1 = m[2];
        FIELDS.put("param1", m[2]);
        if (p1 != "*") {
          if ((m = s.match(/target='([^\']+)' /)) != undefined) {
            FIELDS.put("param2", m[1]);
          }
        }
      }
      break;
    }
  
    case "log": {
      var s = RECORD.getBytes();
      var m;
      if ((m = s.match(/expr='(.+)' lab/)) != undefined) {
        var p = m[1];
        if (
          p.startsWith("done.state.") ||
          p.startsWith("Running ") ||
          p.startsWith("RelativePathURL =") ||
          p.startsWith("Entry1.done event") ||
          p.endsWith("End of this session") ||
          p.startsWith("return values = (") ||
          p.includes("script.done event")
        ) {
          IGNORE_RECORD = true;
        } else {
          FIELDS.put("param", p);
        }
      }
      break;
    }
  
    case "onentry":
    case "state_enter":
    case "state_exit":
    case "event_queued":
    case "event_processed":
    case "onexit": {
      var m;
      if ((m = RECORD.getBytes().match(/name='([^\']+)' /)) != undefined) {
        FIELDS.put("param", m[1]);
      }
      break;
    }
  
    case "fm_exec_error":
    case "exec_error": {
      var m;
      if ((m = RECORD.getBytes().match(/error='([^\']+)' /)) != undefined) {
        FIELDS.put("param", m[1]);
      }
      break;
    }
  
    default:
      break;
  }
