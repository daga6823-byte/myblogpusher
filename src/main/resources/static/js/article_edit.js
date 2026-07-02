function toggleNewCategory() {
	const select = document.getElementById('categorySelect');
	const newInput = document.getElementById('newCategoryName');
	if (select.value === '__new__') {
		newInput.style.display = 'block';
		newInput.focus();
	} else {
		newInput.style.display = 'none';
		newInput.value = '';
	}
}

window.addEventListener('DOMContentLoaded', () => {
	const msg = document.getElementById('savedMessage');
	if (msg) {
		setTimeout(() => { msg.style.display = 'none'; }, 3000);
	}

	const textarea = document.getElementById('content');
	if (textarea && textarea.value.trim() === '') {
		updateFrontMatterFields(true);
	}
});

let contentChanged = false;

const content = document.getElementById('content');
if (content) {
	content.addEventListener('input', () => {
		contentChanged = true;
	});
}

document.getElementById('listButton').addEventListener('click', () => {
	if (contentChanged) {
		const wantSave = confirm('保存していない変更があります。保存しますか？');
		if (wantSave) {
			document.getElementById('redirectTo').value = 'list';
			const form = document.querySelector('form');
			form.action = '/article/save';
			form.submit();
		} else {
			window.location.href = '/article/list';
		}
	} else {
		window.location.href = '/article/list';
	}
});

document.getElementById('backButton').addEventListener('click', () => {
	if (contentChanged) {
		const wantSave = confirm('保存していない変更があります。保存しますか？');
		if (wantSave) {
			document.getElementById('redirectTo').value = 'home';
			const form = document.querySelector('form');
			form.action = '/article/save';
			form.submit();
		} else {
			window.location.href = '/home';
		}
	} else {
		window.location.href = '/home';
	}
});

function updateButtonState() {
	const content = document.getElementById('content').value.trim();
	const categorySelect = document.getElementById('categorySelect').value;
	const newCategoryName = document.getElementById('newCategoryName').value.trim();

	const hasCategory = categorySelect !== '' &&
		(categorySelect !== '__new__' || newCategoryName !== '');
	const hasContent = content !== '';

	const enabled = hasCategory && hasContent;

	document.querySelector('[formaction="/article/save"]').disabled = !enabled;
	document.querySelector('[formaction="/article/correct"]').disabled = !enabled;
}

['categorySelect', 'newCategoryName', 'content'].forEach(id => {
	const el = document.getElementById(id);
	if (el) {
		el.addEventListener('input', updateButtonState);
		el.addEventListener('change', updateButtonState);
	}
});

window.addEventListener('DOMContentLoaded', updateButtonState);

function parseFrontMatter(text) {
	const match = text.match(/^\+\+\+\n([\s\S]*?)\+\+\+\n*/);
	if (!match) return null;
	return match[1];
}

function buildDateString() {
	const now = new Date();
	const offset = -now.getTimezoneOffset();
	const sign = offset >= 0 ? '+' : '-';
	const pad = (n) => String(Math.abs(n)).padStart(2, '0');
	const offsetStr = `${sign}${pad(offset / 60)}:${pad(offset % 60)}`;
	return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}${offsetStr}`;
}

function updateFrontMatterFields(updateDate) {
	const textarea = document.getElementById('content');
	const title = document.getElementById('title').value;
	const categorySelect = document.getElementById('categorySelect').value;
	const newCategoryName = document.getElementById('newCategoryName').value.trim();
	const category = (categorySelect === '__new__') ? newCategoryName : categorySelect;

	const currentText = textarea.value;
	const frontMatterPattern = /^\+\+\+[\s\S]*?\+\+\+\n*/;

	const existingFrontMatterBody = parseFrontMatter(currentText);

	let dateLine;
	if (updateDate || !existingFrontMatterBody) {
		dateLine = `date = '${buildDateString()}'`;
	} else {
		const dateMatch = existingFrontMatterBody.match(/^date = .+$/m);
		dateLine = dateMatch ? dateMatch[0] : `date = '${buildDateString()}'`;
	}

	const bodyText = currentText.replace(frontMatterPattern, '');

	const newFrontMatter =
		`+++
title = '${title}'
${dateLine}
categories = ["${category}"]
draft = false
comments = true
+++
`;

	textarea.value = newFrontMatter + bodyText;
}

document.querySelectorAll('[formaction]').forEach(btn => {
	btn.addEventListener('click', () => {
		updateFrontMatterFields(true);
	});
});

const copyAllButton = document.getElementById('copyAllButton');
if (copyAllButton) {
	copyAllButton.addEventListener('click', async () => {
		const title = document.getElementById('title').value;
		const content = document.getElementById('content').value;
		const combinedText = `${title}\n\n${content}`;

		try {
			await navigator.clipboard.writeText(combinedText);
			const originalLabel = copyAllButton.textContent;
			copyAllButton.textContent = 'コピーしました';
			setTimeout(() => {
				copyAllButton.textContent = originalLabel;
			}, 1500);
		} catch (err) {
			alert('コピーに失敗しました。');
		}
	});
}

//自動保存機能
let workspaceTimer;

function saveWorkspace() {

	const data = {
		title: document.getElementById('title').value,
		content: document.getElementById('content').value
	};

	fetch('/article/workspace/save', {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(data)
	}).catch(err => console.error(err));
}

function scheduleWorkspaceSave() {
	clearTimeout(workspaceTimer);

	workspaceTimer = setTimeout(() => {
		saveWorkspace();
	}, 5000);
}

['title', 'content'].forEach(id => {
	const el = document.getElementById(id);

	if (el) {
		el.addEventListener('input', scheduleWorkspaceSave);
	}
});

document.getElementById('publishButton').addEventListener('click', function() {
	const workId = new URLSearchParams(window.location.search).get('workId');
	const title = document.querySelector('input[name="title"]').value;
	const content = document.querySelector('textarea[name="content"]').value;
	const categorySelect = document.querySelector('select[name="categorySelect"]').value;
	const newCategoryName = document.querySelector('input[name="newCategoryName"]')?.value || '';

	if (!title || !content) {
		alert('タイトルと本文を入力してください');
		return;
	}

	// フォームを作成して投稿確認画面に遷移
	const form = document.createElement('form');
	form.method = 'POST';
	form.action = '/publish/preview';

	const fields = {
		workId: workId || '',
		title: title,
		content: content,
		categorySelect: categorySelect,
		newCategoryName: newCategoryName
	};

	for (const [key, value] of Object.entries(fields)) {
		const input = document.createElement('input');
		input.type = 'hidden';
		input.name = key;
		input.value = value;
		form.appendChild(input);
	}

	document.body.appendChild(form);
	form.submit();
});

document.getElementById('imageButton').addEventListener('click', function() {
	fetch('/article/images')
		.then(res => res.json())
		.then(images => {
			const list = document.getElementById('imageList');
			list.innerHTML = '';
			images.forEach(img => {
				const div = document.createElement('div');
				div.style.cursor = 'pointer';
				div.innerHTML = `<img src="${img}" style="width: 100%; height: 150px; object-fit: cover;" onclick="insertImage('${img}')">`;
				list.appendChild(div);
			});
			document.getElementById('imageModal').style.display = 'block';
		});
});

function insertImage(url) {
	const textarea = document.querySelector('textarea[name="content"]');
	textarea.value += '\n![image](' + url + ')\n';
	document.getElementById('imageModal').style.display = 'none';
}

// セッション維持
setInterval(() => {
	fetch('/article/session/keepalive', {
		method: 'POST'
	}).catch(err => console.error(err));
}, 10 * 60 * 1000); // 10分ごと

//コードブロック
document.getElementById('insertMenuButton').addEventListener('click', function() {
	const menu = document.getElementById('insertMenu');
	menu.style.display = menu.style.display === 'none' ? 'block' : 'none';
});

document.getElementById('codeBlockButton').addEventListener('click', function() {
	document.getElementById('insertMenu').style.display = 'none';
	document.getElementById('codeBlockEditor').style.display = 'block';
	document.getElementById('codeInput').focus();
});

document.getElementById('insertCodeButton').addEventListener('click', function() {
	const code = document.getElementById('codeInput').value;
	const textarea = document.getElementById('content');
	const start = textarea.selectionStart;

	const codeBlock = '\n```\n' + code + '\n```\n';

	textarea.value =
		textarea.value.substring(0, start) +
		codeBlock +
		textarea.value.substring(start);

	document.getElementById('codeInput').value = '';
	document.getElementById('codeBlockEditor').style.display = 'none';
	textarea.focus();
});

document.getElementById('cancelCodeButton').addEventListener('click', function() {
	document.getElementById('codeInput').value = '';
	document.getElementById('codeBlockEditor').style.display = 'none';
});
