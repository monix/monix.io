$(document).ready(function() {
  $('#toc').toc({
    title: '',
    listType: 'ul',
    showSpeed: 0
  });

  // Showing the TOC breaks navigation
  var hash = window.location.hash;
  if (hash) {
    window.location.hash = hash;
  }
});
