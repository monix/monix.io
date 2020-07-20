require "nokogiri"

def to_absolute_url(site, url)
  if url =~ /^\//
    site['url'] + site['baseurl'] + url
  else
    url
  end
end

module Jekyll
  module MyGenericUtils
    @@site = Jekyll.configuration({})
    @@pages_cache = {}

    def api_base_url(path)
      if path.include?("docs/3x/")
        @@site['api3x']
      elsif path.include?("docs/2x/")
        @@site['api2x']
      else 
        @@site['apiCurrent']
      end
    end

    def doc_contents_link(path)
      if path =~ /\/docs\//
        path.sub(/docs\/([^\/]+)(\/[^$]*)$/, "docs/current/$1")
      else
        "docs/current/index.md"
      end
    end

    def current_doc_link(path, pages, version=nil)
      if @@pages_cache.length == 0
        pages.each do |p|
          @@pages_cache[p.path] = true
        end
      end
      
      p = path.sub(/^docs\/[^\/]+/, "docs/#{version || "current"}")
      if @@pages_cache.key?(p)
        p
      else
        path
      end
    end

    def rss_campaign_link(link, keyword)
      l = if link.include? '?'
        link + "&"
      else
        link + "?"
      end

      l = l + "pk_campaign=rss"
      l = l + "&pk_kwd=" + keyword if keyword
      l
    end

    def rss_summary(post)
      html = post.excerpt
      doc = Nokogiri::HTML(html)
    
      doc.css("h1").each{|elem|
        elem["style"] = "font-size: 180%; font-weight: bold;"
        elem.inner_html = "<b>" + elem.inner_html + "<b>"
      }
      doc.css("h2").each{|elem|
        elem["style"] = "font-size: 150%; font-weight: bold;"
        elem.inner_html = "<b>" + elem.inner_html + "<b>"
      }
      doc.css("h3").each{|elem|
        elem["style"] = "font-size: 120%; font-weight: bold;"
        elem.inner_html = "<b>" + elem.inner_html + "<b>"
      }  

      doc.css("img[class=right]").each{|elem|
        elem["style"] = "float: right; margin-left: 10px; margin-bottom: 10px;"
        elem["align"] = "right"
      }
    
      doc.css("img[class=left]").each{|elem|
        elem["style"] = "float: left; margin-right: 10px; margin-bottom: 10px;"
        elem["align"] = "left"
      }
  
      doc.css("img[class=center]").each{|elem|
        elem["style"] = "display: block; margin: auto;"
        elem["align"] = "center"
      }
  
      doc.css("img").each{|elem|
        sep = elem["style"] !~ /;\s*$/ ? "; " : ""
        sep = " " if sep == "" && elem["style"] !~ /\s+$/
        
        if elem["width"] && elem["style"] !~ /width[:]/
          elem["style"] += sep + "width: " + elem["width"] + "px; " 
        end 
        if elem["height"] && elem["style"] !~ /height[:]/
          elem["style"] += sep + "height: " + elem["height"] + "px; "
        end

        elem["style"] = elem["style"].strip
      }

      doc.css("a").each{|elem|
        elem["href"] = to_absolute_url(@@site, elem['href'])
        if elem["href"].include?("monix.io")
          elem["href"] = rss_campaign_link(elem["href"], "inner-link")
        end
      }

      body = doc.at_css("body")
      body ? body.inner_html : ""
    end
  end
end

Liquid::Template.register_filter(Jekyll::MyGenericUtils)
