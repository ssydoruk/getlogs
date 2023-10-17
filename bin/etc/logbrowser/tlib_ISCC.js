var m;

for(var s of RECORD.getBytes().split('\n')){
    if( (m=s.match(/AttributePrivateMsgID\s*(\d*)$/i))!=undefined){
        FIELDS.put('PrivateMsgID', 'msgid='+m[1]);
    }
    else if( (m=s.match(/AttributeLocation\s(\S*)$/i))!=undefined){
        FIELDS.put('location', 'loc='+m[1]);
    }
    else if( (m=s.match(/(from ISCC):/i))!=undefined){
        FIELDS.put('source', m[1]);
    }
    else if( (m=s.match(/ISCCAttributeNetworkDestDN\s(\S*)$/i))!=undefined){
        FIELDS.put('destDN', 'dst='+m[1]);
    }
    else if( (m=s.match(/ISCCAttributeCallDataXferType\s(\S*)$/i))!=undefined){
        FIELDS.put('xferType', 'xfertype:'+m[1]);
    }				
    else if( (m=s.match(/ISCCAttributeCallType\s(\S*)$/i))!=undefined){
        FIELDS.put('calltype', 'calltype:'+m[1]);
    }				
    else if( (m=s.match(/.+AttributeError(?:Message|Code)\t(.*)$/i))!=undefined){
        FIELDS.put('calltype', 'calltype:'+m[1]);
    }				
}