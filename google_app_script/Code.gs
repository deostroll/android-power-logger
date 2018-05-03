function doPost(e) {
  LockService.getScriptLock().waitLock(1500);
  Logger.log("Acquired log");
  return processPost(e)
}

function doGet(e) {
  return processGet(e);
}

//NOTE: plug your sheet id here...
var SHEET_ID = "18yQIaEc14Naub8qjlxEEG2-k1xZZND16-DEwtf9yM-M"

// obsolete function
function process(e) {
  var ss = SpreadsheetApp.openById(SHEET_ID);
  var sheet = ss.getSheetByName("Sheet1");
  var postData = JSON.parse(e.postData.contents);

  var status = postData.status;
  var battery = postData.battery;

  sheet.insertRows(2);

  var dateString = postData.dateString;
  var timeString = postData.timeString;
  var now = new Date();

  sheet.getRange(2, 1, 1, 5).setValues([
    [status, battery, dateString, timeString, now]
  ]);
  return ContentService.createTextOutput(JSON.stringify({
    "success": true
  })).setMimeType(ContentService.MimeType.JSON);
}

// this is for pinging only and quering also
function processGet(e) {
  var qs = parseQueryString(e.queryString);
  if (qs.op && qs.op === 'verify') {
    var count = parseInt(qs.count);
    var ids = [];
    var ss = SpreadsheetApp.openById(SHEET_ID);
    var sheet = ss.getSheetByName("Sheet1");
    var ids = sheet.getRange(2, 1, count, 6).getValues().map(function(row) {
      return row[5];
    });
    var payload = JSON.stringify(ids);
    return ContentService.createTextOutput(payload).setMimeType(ContentService.MimeType.JSON);
  } else if (qs.op && qs.op === 'ping') {
    var ss = SpreadsheetApp.openById(SHEET_ID);
    var sheet = ss.getSheetByName("Sheet2");
    sheet.insertRows(1);
    sheet.getRange(1, 1, 1, 2).setValues([
      [
        qs.battery,
        new Date()
      ]
    ]);
    return;
  }
  return ContentService.createTextOutput(JSON.stringify({
    success: true,
    date: new Date()
  })).setMimeType(ContentService.MimeType.JSON);
}

// new function for batch inserting
function processPost(e) {
  var ss = SpreadsheetApp.openById(SHEET_ID);
  var sheet = ss.getSheetByName("Sheet1");
  //    var scriptLock = LockService.getScriptLock();
  //    var lockSuccess = scriptLock.tryLock(3000);
  lockSuccess = true;
  if (lockSuccess) {

    var contents = JSON.parse(e.postData.contents);
    var recordsCount = doProcessing(sheet, contents);

    //        scriptLock.releaseLock();

    var resp = ContentService.createTextOutput()
      .setContent(JSON.stringify({
        success: lockSuccess,
        added: recordsCount
      }))
      .setMimeType(ContentService.MimeType.JSON);

    return resp;
  } else {
    return ContentService.createTextOutput()
      .setContent(JSON.strigify({
        success: lockSuccess,
        reason: 'Could not acquire lock'
      }))
      .setMimeType(ContentService.MimeType.JSON);
  }

}

//obsolete
function addToSheet(sheet, postData) {
  var status = postData.status;
  var battery = postData.battery;

  sheet.insertRows(2);

  var dateString = postData.dateString;
  var timeString = postData.timeString;
  var now = new Date();

  sheet.getRange(2, 1, 1, 6).setValues([
    [status, battery, dateString, timeString, now, postData.id]
  ]);
}

function doProcessing(sheet, data) {

  // 1. Geting all existing ids
  var lastRow = sheet.getLastRow();
  var existingData = sheet.getRange(2, 1, lastRow - 1 || 1, 6).getValues();
  var existingIds = existingData.map(function(row) {
    return row[5];
  });

  // 2. adding entries post check
  var entries = data.entries;
  var total = entries.length;
  var now = new Date();

  for (var i = 0; i < total; i++) {
    var item = entries[i];
    if (existingIds.indexOf(item.id) === -1) {
      var status = item.status;
      var battery = item.battery;
      var dateString = item.dateString;
      var timeString = item.timeString;
      var id = item.id;

      // 2.1 adding new row
      sheet.insertRows(2);
      // 2.2 adding the data to row
      var newRowRange = sheet.getRange(2, 1, 1, 6);
      newRowRange.setValues([
        [status, battery, dateString, timeString, now, id]
      ])
    }
  }

  return total;
}

function test() {
  var data = [{
      status: 'on',
      battery: 85,
      dateString: '20160504',
      timeString: '21:22',
      id: 1
    },
    {
      status: 'off',
      battery: 86,
      dateString: '20160504',
      timeString: '21:23',
      id: 2
    },
    {
      status: 'on',
      battery: 84,
      dateString: '20160504',
      timeString: '21:26',
      id: 3
    },
  ];

  var contents = {
    entries: data
  };
  var mockRequest = {
    postData: {
      contents: JSON.stringify(contents)
    }
  };

  processPost(mockRequest);
}

function parseQueryString(text) {
  var obj = {};
  var components = text.split('&');
  for (var i = 0, j = components.length; i < j; i++) {
    var c = components[i].split('=');
    obj[c[0]] = c[1];
  }

  return obj;
}

function testRange() {
  var ss = SpreadsheetApp.openById(SHEET_ID);
  var sheet = ss.getSheetByName("Sheet1");
  var data = sheet.getRange(2, 1, sheet.getLastRow() - 1, 6).getValues();
  Logger.log(JSON.stringify(data, null, 2));
}
