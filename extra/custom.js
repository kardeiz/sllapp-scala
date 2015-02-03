function formatDateString(date) {
  return [
    date.getUTCFullYear(),
    pad(date.getUTCMonth() + 1),
    pad(date.getUTCDate())
  ].join('-');
}

$(document).ready(function() {
  $('#datepicker').datepicker({
    format: "yyyy-mm-dd",
    autoclose: true
  });
});
