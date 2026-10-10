/**
 * インデックスのワークテーブル一覧における日時表示を担当するJavaScript
 */

/**
 * UTC日時をユーザー環境のタイムゾーンで表示する。
 */
function convertLocalDate() {

    document.querySelectorAll('.local-date')
        .forEach(element => {

            const date =
                new Date(
                    element.dataset.date + 'Z'
                );

            if (isNaN(date.getTime())) {
                return;
            }

            element.textContent =
                date.toLocaleString(
                    undefined,
                    {
                        year: 'numeric',
                        month: '2-digit',
                        day: '2-digit',
                        hour: '2-digit',
                        minute: '2-digit'
                    }
                );

        });

}

/**
 * インデックス一覧の初期化。
 */
function initializeIndexList() {

    convertLocalDate();

}

if (document.readyState === 'loading') {

    document.addEventListener(
        'DOMContentLoaded',
        initializeIndexList
    );

} else {

    initializeIndexList();

}

console.log('index_list.js loaded');
