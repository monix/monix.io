module Jekyll
  module VideoLinks
    def vimeo_link(uid)
      "https://vimeo.com/#{uid}"
    end

    def vimeo_player_link(uid)
      "https://player.vimeo.com/video/#{uid}?title=0&byline=0&portrait=0&dnt=1"
    end

    def vimeo_raw_link(uid)
      "https://videos.monix.io/redirect/#{uid}/HD%201080p?download=false&fallback=true"
    end

    def vimeo_thumb_link(uid)
      "https://videos.monix.io/thumb/#{uid}"
    end

    def vimeo_thumb_play_link(uid)
      "https://videos.monix.io/thumb-play/#{uid}"
    end

    def youtube_link(uid)
      "https://www.youtube.com/watch?v=#{uid}"
    end

    def youtube_player_link(uid)
      "https://www.youtube-nocookie.com/embed/#{uid}"
    end

    def youtube_thumb_link(uid)
      "https://img.youtube.com/vi/#{uid}/maxresdefault.jpg"
    end
  end
end

Liquid::Template.register_filter(Jekyll::VideoLinks)
