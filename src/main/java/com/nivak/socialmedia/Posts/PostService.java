package com.nivak.socialmedia.Posts;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class PostService {
    
    @Autowired
    private PostRepository postRepository;

    @Autowired
    private void myPost(@Qualifier("posts") PostRepository postRepository){
        this.postRepository = postRepository;
    }
    
    public List<Post> allPost(){
        return postRepository.findAll();
    }
}
