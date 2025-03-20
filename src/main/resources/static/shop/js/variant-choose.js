$(document).on('click', function (e) {
  let target = e.target;

  if (target.closest('.variant-option .item')) {
    const variantGroup = $(target).closest('.variant-option');
    variantGroup.find('.item').removeClass('variant-choose');
    $(target).addClass('variant-choose');
    
    const variantType = variantGroup.data('variant-type');
    const variantId = $(target).data('variant-id');
    const variantName = $(target).data('variant-name');

    $(`#variant-${variantType}`).text(variantName);
    updateSelectedVariant(variantType, variantId, variantName);
    checkVariantAvailability();
  }
});

function updateSelectedVariant(type, id, name) {
  if (!window.selectedVariants) {
    window.selectedVariants = {};
  }
  window.selectedVariants[type] = {
    id: id,
    name: name
  };
}

function checkVariantAvailability() {
  const allVariantsSelected = $('.variant-option').length === Object.keys(window.selectedVariants || {}).length;
  
  if (allVariantsSelected) {
    // Kiểm tra tồn kho của biến thể
    const productId = $('#product-id').val();
    const variantIds = Object.values(window.selectedVariants).map(v => v.id);
    
    $.ajax({
      url: '/api/check-variant-availability',
      method: 'POST',
      data: JSON.stringify({
        productId: productId,
        variantIds: variantIds
      }),
      contentType: 'application/json',
      success: function(response) {
        if (response.available) {
          $('.not-found-variant').hide();
          $('#btn-buy-now').show();
        } else {
          $('.not-found-variant').show();
          $('#btn-buy-now').hide();
        }
      }
    });
  }
}

// Xử lý modal hướng dẫn chọn biến thể
$('.variant-guide').click(function () {
  $('#variantGuideModal').modal('show');
}); 