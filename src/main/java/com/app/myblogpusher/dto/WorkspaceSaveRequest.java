package com.app.myblogpusher.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkspaceSaveRequest {	
	
	private Long categoryGroupId;
	private String title;
	private String content;
}
