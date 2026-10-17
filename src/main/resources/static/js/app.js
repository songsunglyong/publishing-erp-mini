$(function () {
  $.ajaxSetup({headers: {'X-CSRF-TOKEN': $('meta[name="csrf-token"]').attr('content')}});
  $('form[data-confirm]').on('submit', function (event) {
    if (!window.confirm($(this).attr('data-confirm'))) event.preventDefault();
  });
});
