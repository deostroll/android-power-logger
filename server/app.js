var express = require('express');
var util = require('util');
var app = express();

app.get('/power/ON', function(req, res){
  var msg = util.format('%s : %s', new Date(), "ON");
  console.log(msg);
  res.status(200).send('Ok');
});

app.get('/power/OFF', function(req, res){
  var msg = util.format('%s : %s', new Date(), "OFF");
  console.log(msg);
  res.status(200).send('Ok');
});

app.listen(3000, function(err){
  if (err) {
    console.log('error:', err)
  }
  else {
    console.log('Listening on port 3000')
  }
})
