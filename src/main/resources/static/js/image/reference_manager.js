/**
 * 参考文献の共通処理を担当するモジュール
 *
 * 参考文献カテゴリー・一覧の取得、一覧表示、絞り込み、
 * 参考文献の登録を担当する。
 * モーダルの開閉や脚注の本文挿入など、画面固有の処理は担当しない。
 */
window.ReferenceManager = {

	referenceCache: [],

	referenceCategoriesLoaded: false,

	/**
	 * 参考文献カテゴリーの選択肢を取得する。
	 */
	async loadCategories() {

		const select = document.getElementById('referenceCategorySelect');

		if (!select) {
			return;
		}

		if (this.referenceCategoriesLoaded) {
			return;
		}

		select.innerHTML = '';

		const allOption = document.createElement('option');
		allOption.value = '';
		allOption.textContent = 'すべて';
		select.appendChild(allOption);

		const response = await fetch('/category/reference/categories');

		if (!response.ok) {
			alert('参考文献カテゴリーの取得に失敗しました');
			return;
		}

		const categories = await response.json();

		categories.forEach(category => {

			const option = document.createElement('option');
			option.value = category.categoryId;
			option.textContent = category.categoryName;
			select.appendChild(option);

		});

		this.referenceCategoriesLoaded = true;

	},

	/**
	 * 指定されたカテゴリーグループの参考文献を取得する。
	 */
	async loadReferences(groupId, focusInsertButton = false) {

		if (!groupId) {
			alert('カテゴリー情報を取得できません');
			return false;
		}

		const response = await fetch(
			`/category/reference/list?groupId=${encodeURIComponent(groupId)}`
		);

		if (!response.ok) {
			alert('参考文献の取得に失敗しました');
			return false;
		}

		this.referenceCache = await response.json();

		this.displayReferences(this.referenceCache, focusInsertButton);

		return true;

	},

	/**
	 * 取得済みの参考文献を一覧表示する。
	 */
	displayReferences(references, focusInsertButton = false) {

		const list = document.getElementById('referenceList');

		if (!list) {
			return;
		}

		list.innerHTML = '';

		references.forEach(reference => {

			const label = document.createElement('label');
			const checkbox = document.createElement('input');

			checkbox.type = 'checkbox';
			checkbox.name = 'referenceCheck';
			checkbox.value = JSON.stringify(reference);

			checkbox.addEventListener('change', () => {

				if (checkbox.checked && focusInsertButton) {

					document
						.getElementById('insertReferenceButton')
						?.focus();

				}

			});

			label.appendChild(checkbox);
			label.appendChild(
				document.createTextNode(reference.referenceName)
			);

			list.appendChild(label);
			list.appendChild(document.createElement('br'));

		});

	},

	/**
	 * 取得済みの参考文献をカテゴリーで絞り込む。
	 */
	filterReferences(categoryId, focusInsertButton = false) {

		if (!categoryId) {
			this.displayReferences(
				this.referenceCache,
				focusInsertButton
			);
			return;
		}

		this.displayReferences(
			this.referenceCache.filter(
				reference =>
					String(reference.categoryId) === String(categoryId)
			),
			focusInsertButton
		);

	},

	/**
	 * 参考文献を登録する。
	 *
	 * parameterName / parameterValue には、
	 * 登録APIに渡すカテゴリー指定パラメータを指定する。
	 * groupId は登録後の一覧再取得に使用する。
	 */
	async saveReference({
		parameterName,
		parameterValue,
		groupId,
		headers = {}
	}) {

		const referenceName =
			document.getElementById('referenceName').value.trim();

		const url =
			document.getElementById('referenceUrl').value.trim();

		if (!parameterValue) {
			alert('カテゴリー情報を取得できません');
			return false;
		}

		if (!referenceName) {
			alert('参考文献名を入力してください');
			return false;
		}

		const body = new URLSearchParams();

		body.append(parameterName, parameterValue);
		body.append('referenceName', referenceName);
		body.append('url', url);

		const response = await fetch('/category/reference/save', {

			method: 'POST',

			headers: {
				'Content-Type': 'application/x-www-form-urlencoded',
				...headers
			},

			body: body.toString()

		});

		if (response.status === 409) {
			const error = await response.json();
			alert(error.message);
			return false;
		}

		if (!response.ok) {
			alert('参考文献の登録に失敗しました');
			return false;
		}

		document.getElementById('referenceName').value = '';
		document.getElementById('referenceUrl').value = '';

		return await this.loadReferences(groupId);

	}

};