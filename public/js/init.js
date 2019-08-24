function initVimeo(uid) {
  $(document).ready(function () {
    // Building the list
    function add(list, item) {
      var kind = encodeURIComponent(item.public_name.toLowerCase())
      list.append(
        "<li>" +
        "<a href='https://videos.monix.io/redirect/" + uid + "/" + kind + "/?download=false' target='_blank' rel='nofollow'>" +
        item.public_name + "</a> (" + item.size + ")" +
        "</li>"
      );
    }

    $.ajax({
      url: "https://videos.monix.io/get/" + uid,
      dataType: 'json',
      success: function (data) {
        if (data.allow_downloads) {
          var list = $("#video-download > ul");
          var added = false;

          if (data.files) {
            for (var i = 0; i < data.files.length; i++) {
              var item = data.files[i];
              add(list, item);
              added = true;
            }
          }
          if (data.source_file) {
            add(list, data.source_file);
            added = true;
          }

          if (added) {
            $("#video-download").show();
          }
        }
      }
    });
  });
}

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

  // ------------------------------------------------------------------------

  function vimeoTriggerVideo() {
    var playButton = $('#vimeo .play-button');
    var src = playButton.attr('data-src');
    var ratio = playButton.attr('data-ratio');

    $('#vimeo').html(
      '<div id="video-frame" class="presentation" style="padding-bottom: ' + ratio + '%;">\n' +
      '  <iframe src="' + src + '&autoplay=1" id="vimeo-iframe" frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen allow="autoplay; fullscreen"></iframe>\n' +
      '</div>'
    );
  }

  $('#vimeo a').click(function (e) {
    e.preventDefault();
    vimeoTriggerVideo();
  });
});
