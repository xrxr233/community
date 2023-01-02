$(function(){
	$("#publishBtn").click(publish);
});

function publish() {
	//隐藏发布帖子的弹出框
	$("#publishModal").modal("hide");

	//发送AJAX前，将csrf令牌设置到请求的消息头中
	// var token = $("meta[name='_csrf']").attr("content");
	// var header = $("meta[name='_csrf_header']").attr("content");
	// $(document).ajaxSend(function (e, xhr, options) {
	// 	xhr.setRequestHeader(header, token);
	// });

	//获取帖子标题和内容
	var title = $("#recipient-name").val();
	var content = $("#message-text").val();

	//发送异步请求（POST）
	$.post(
		CONTEXT_PATH + "/discuss/add",
		{"title":title,"content":content},
		function (data) {
			data = $.parseJSON(data);
			//在提示框中填写返回的消息
			$("#hintBody").text(data.msg);
			//显示提示框
			$("#hintModal").modal("show");
			//2s后自动隐藏提示框
			setTimeout(function(){
				$("#hintModal").modal("hide");
				//发帖成功，刷新页面显示最新的帖子
				if(data.code == 0) {
					window.location.reload();
				}
			}, 2000);
		}
	);

}