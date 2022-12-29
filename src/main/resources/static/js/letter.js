$(function(){
	$("#sendBtn").click(send_letter);
	$(".closemessage").click(delete_msg);
});

function send_letter() {
	//关闭填写框
	$("#sendModal").modal("hide");

	var toName =$("#recipient-name").val();
	var content = $("#message-text").val();
	$.post(
		CONTEXT_PATH + "/letter/send",
		{"toName":toName,"content":content},
		function (data) {
			data = $.parseJSON(data);
			if(data.code == 0) {
				$("#hintBody").text("发送成功！");
			}else {
				$("#hintBody").text(data.msg);
			}

			//显示提示框
			$("#hintModal").modal("show");
			setTimeout(function(){
				$("#hintModal").modal("hide");
				location.reload();
			}, 2000);
		}
	);
}

function delete_msg() {
	var btn = this;
	var id = $(btn).prev().val();

	$.post(
		CONTEXT_PATH + "/letter/delete",
		{"id":id},
		function (data) {
			data = $.parseJSON(data);
			if(data.code == 0) {
				$(btn).parents(".media").remove();
			}else {
				alert(data.msg);
			}
		}
	);
}