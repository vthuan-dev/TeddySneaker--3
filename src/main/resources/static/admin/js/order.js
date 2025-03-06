// Định nghĩa function trong window object để có thể gọi từ mọi nơi
window.exportInvoice = function(orderId) {
    $.ajax({
        url: '/api/admin/invoices/export/' + orderId,
        type: 'GET',
        xhrFields: {
            responseType: 'blob'
        },
        success: function(response) {
            const blob = new Blob([response], { type: 'application/pdf' });
            const downloadUrl = window.URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = downloadUrl;
            a.download = "invoice-" + orderId + ".pdf";
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(downloadUrl);
            a.remove();
        },
        error: function(xhr) {
            if (xhr.status === 404) {
                toastr.error('Không tìm thấy hóa đơn cho đơn hàng này');
            } else {
                toastr.error('Có lỗi xảy ra khi xuất hóa đơn');
            }
        }
    });
}; 