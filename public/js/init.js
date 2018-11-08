function initVimeoDownloadLinks(uid) {
  $(document).ready(function () {
    function add(list, item) {
      list.append(
        "<li>" +
        "<a href='https://videos.monix.io/redirect/" + uid + "/" + encodeURI(item.public_name) + "/?download=false' target='_blank' rel='nofollow'>" +
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
    })    
  });
}

$(document).ready(function() {
    $('#toc').toc({
        title: '',
        listType: 'ul',
        showSpeed: 0
    });
});
