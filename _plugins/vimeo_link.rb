module Jekyll
  module VimeoLink
    def vimeo_link(uid)
      "https://vimeo.com/#{uid}"
    end

    def vimeo_player_link(uid)
      "https://player.vimeo.com/video/#{uid}?title=0&byline=0&portrait=0"
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
  end
end

Liquid::Template.register_filter(Jekyll::VimeoLink)

