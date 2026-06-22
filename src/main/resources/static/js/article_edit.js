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
});

let contentChanged = false;

['categorySelect', 'newCategoryName', 'title', 'content'].forEach(id => {
    const el = document.getElementById(id);
    if (el) {
        el.addEventListener('input', () => { contentChanged = true; });
        el.addEventListener('change', () => { contentChanged = true; });
    }
});

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
