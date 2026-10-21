$(function () {
  function renumber() {
    $('#order-lines tr').each(function (index) {
      $(this).find('[name]').each(function () {
        this.name = this.name.replace(/items\[\d+\]/, 'items[' + index + ']');
        this.removeAttribute('id');
      });
    });
  }
  $('#add-order-line').on('click', function () {
    if ($('#order-lines tr').length >= 20) return;
    var row = $('#order-lines tr').first().clone(false);
    row.find('select').val(''); row.find('input').val('');
    row.find('input[name$=".quantity"]').val('1');
    $('#order-lines').append(row); renumber();
  });
  $('#order-lines').on('click', '.remove-line', function () {
    if ($('#order-lines tr').length > 1) { $(this).closest('tr').remove(); renumber(); }
  });
});
