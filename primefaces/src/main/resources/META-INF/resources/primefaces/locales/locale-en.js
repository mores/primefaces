 /** English */
 PrimeFaces.locales['en'] = {
     closeText: 'Close',
     prevText: 'Previous',
     nextText: 'Next',
     monthNames: ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'],
     monthNamesShort: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],
     dayNames: ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'],
     dayNamesShort: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
     dayNamesMin: ['S', 'M', 'T', 'W ', 'T', 'F ', 'S'],
     weekHeader: 'Week',
     weekNumberTitle: 'W',
     firstDay: 0,
     isRTL: false,
     showMonthAfterYear: false,
     yearSuffix: '',
     timeOnlyTitle: 'Only Time',
     timeText: 'Time',
     hourText: 'Hour',
     minuteText: 'Minute',
     secondText: 'Second',
     millisecondText: 'Millisecond',
     currentText: 'Current Date',
     ampm: false,
     year: 'Year',
     month: 'Month',
     week: 'Week',
     day: 'Day',
     list: 'Agenda',
     allDayText: 'All Day',
     moreLinkText: 'More...',
     noEventsText: 'No Events',
     today: 'Today',
     clear: 'Clear',
     aria: {
         'paginator.PAGE': 'Page {0}',
         'calendar.BUTTON': 'Show Calendar',
         'datatable.sort.ASC': 'activate to sort column ascending',
         'datatable.sort.DESC': 'activate to sort column descending',
         'datatable.sort.NONE': 'activate to remove sorting on column',
         'columntoggler.CLOSE': 'Close',
         'overlaypanel.CLOSE': 'Close'
     },
     messages: { //optional for Client Side Validation
         'javax.faces.component.UIInput.REQUIRED': '{0}: Validation Error: Value is required.',
         'javax.faces.converter.IntegerConverter.INTEGER': '{2}: \'{0}\' must be a number consisting of one or more digits.',
         'javax.faces.converter.IntegerConverter.INTEGER_detail': '{2}: \'{0}\' must be a number between -2147483648 and 2147483647. Example: {1}.',
         'javax.faces.converter.DoubleConverter.DOUBLE': '{2}: \'{0}\' must be a number consisting of one or more digits.',
         'javax.faces.converter.DoubleConverter.DOUBLE_detail': '{2}: \'{0}\' must be a number between 4.9E-324 and 1.7976931348623157E308. Example: {1}.',
         'javax.faces.converter.BigDecimalConverter.DECIMAL': '{2}: \'{0}\' must be a signed decimal number.',
         'javax.faces.converter.BigDecimalConverter.DECIMAL_detail': '{2}: \'{0}\' must be a signed decimal number consisting of zero or more digits, that may be followed by a decimal point and fraction. Example: {1}.',
         'javax.faces.converter.BigIntegerConverter.BIGINTEGER': '{2}: \'{0}\' must be a number consisting of one or more digits.',
         'javax.faces.converter.BigIntegerConverter.BIGINTEGER_detail': '{2}: \'{0}\' must be a number consisting of one or more digits. Example: {1}.',
         'javax.faces.converter.ByteConverter.BYTE': '{2}: \'{0}\' must be a number between 0 and 255.',
         'javax.faces.converter.ByteConverter.BYTE_detail': '{2}: \'{0}\' must be a number between 0 and 255. Example: {1}.',
         'javax.faces.converter.CharacterConverter.CHARACTER': '{1}: \'{0}\' must be a valid character.',
         'javax.faces.converter.CharacterConverter.CHARACTER_detail': '{1}: \'{0}\' must be a valid ASCII character.',
         'javax.faces.converter.ShortConverter.SHORT': '{2}: \'{0}\' must be a number consisting of one or more digits.',
         'javax.faces.converter.ShortConverter.SHORT_detail': '{2}: \'{0}\' must be a number between -32768 and 32767. Example: {1}.',
         'javax.faces.converter.BooleanConverter.BOOLEAN': '{1}: \'{0}\' must be \'true\' or \'false\'.',
         'javax.faces.converter.BooleanConverter.BOOLEAN_detail': '{1}: \'{0}\' must be \'true\' or \'false\'.  Any value other than \'true\' will evaluate to \'false\'.',
         'javax.faces.validator.LongRangeValidator.MAXIMUM': '{1}: Validation Error: Value is greater than allowable maximum of \'{0}\'.',
         'javax.faces.validator.LongRangeValidator.MINIMUM': '{1}: Validation Error: Value is less than allowable minimum of \'{0}\'.',
         'javax.faces.validator.LongRangeValidator.NOT_IN_RANGE': '{2}: Validation Error: Specified attribute is not between the expected values of {0} and {1}.',
         'javax.faces.validator.LongRangeValidator.TYPE={0}': 'Validation Error: Value is not of the correct type.',
         'javax.faces.validator.DoubleRangeValidator.MAXIMUM': '{1}: Validation Error: Value is greater than allowable maximum of \'{0}\'.',
         'javax.faces.validator.DoubleRangeValidator.MINIMUM': '{1}: Validation Error: Value is less than allowable minimum of \'{0}\'.',
         'javax.faces.validator.DoubleRangeValidator.NOT_IN_RANGE': '{2}: Validation Error: Specified attribute is not between the expected values of {0} and {1}.',
         'javax.faces.validator.DoubleRangeValidator.TYPE={0}': 'Validation Error: Value is not of the correct type.',
         'javax.faces.converter.FloatConverter.FLOAT': '{2}: \'{0}\' must be a number consisting of one or more digits.',
         'javax.faces.converter.FloatConverter.FLOAT_detail': '{2}: \'{0}\' must be a number between 1.4E-45 and 3.4028235E38  Example: {1}.',
         'javax.faces.converter.DateTimeConverter.DATE': '{2}: \'{0}\' could not be understood as a date.',
         'javax.faces.converter.DateTimeConverter.DATE_detail': '{2}: \'{0}\' could not be understood as a date. Example: {1}.',
         'javax.faces.converter.DateTimeConverter.TIME': '{2}: \'{0}\' could not be understood as a time.',
         'javax.faces.converter.DateTimeConverter.TIME_detail': '{2}: \'{0}\' could not be understood as a time. Example: {1}.',
         'javax.faces.converter.DateTimeConverter.DATETIME': '{2}: \'{0}\' could not be understood as a date and time.',
         'javax.faces.converter.DateTimeConverter.DATETIME_detail': '{2}: \'{0}\' could not be understood as a date and time. Example: {1}.',
         'javax.faces.converter.DateTimeConverter.PATTERN_TYPE': '{1}: A \'pattern\' or \'type\' attribute must be specified to convert the value \'{0}\'.',
         'javax.faces.converter.NumberConverter.CURRENCY': '{2}: \'{0}\' could not be understood as a currency value.',
         'javax.faces.converter.NumberConverter.CURRENCY_detail': '{2}: \'{0}\' could not be understood as a currency value. Example: {1}.',
         'javax.faces.converter.NumberConverter.PERCENT': '{2}: \'{0}\' could not be understood as a percentage.',
         'javax.faces.converter.NumberConverter.PERCENT_detail': '{2}: \'{0}\' could not be understood as a percentage. Example: {1}.',
         'javax.faces.converter.NumberConverter.NUMBER': '{2}: \'{0}\' could not be understood as a number.',
         'javax.faces.converter.NumberConverter.NUMBER_detail': '{2}: \'{0}\' could not be understood as a number. Example: {1}.',
         'javax.faces.converter.NumberConverter.PATTERN': '{2}: \'{0}\' could not be understood as a number pattern.',
         'javax.faces.converter.NumberConverter.PATTERN_detail': '{2}: \'{0}\' could not be understood as a number pattern. Example: {1}.',
         'javax.faces.validator.LengthValidator.MINIMUM': '{1}: Validation Error: Length is less than allowable minimum of \'{0}\'.',
         'javax.faces.validator.LengthValidator.MAXIMUM': '{1}: Validation Error: Length is greater than allowable maximum of \'{0}\'.',
         'javax.faces.validator.RegexValidator.PATTERN_NOT_SET': 'Regex pattern must be set.',
         'javax.faces.validator.RegexValidator.PATTERN_NOT_SET_detail': 'Regex pattern must be set to non-empty value.',
         'javax.faces.validator.RegexValidator.NOT_MATCHED': 'Regex Pattern not matched.',
         'javax.faces.validator.RegexValidator.NOT_MATCHED_detail': 'Regex pattern of \'{0}\' not matched.',
         'javax.faces.validator.RegexValidator.MATCH_EXCEPTION': 'Error in regular expression.',
         'javax.faces.validator.RegexValidator.MATCH_EXCEPTION_detail': 'Error in regular expression, \'{0}\'.',
         //optional for bean validation integration in client side validation
         'javax.faces.validator.BeanValidator.MESSAGE': '{0}',
         'javax.validation.constraints.AssertFalse.message': 'must be false.',
         'javax.validation.constraints.AssertTrue.message': 'must be true.',
         'javax.validation.constraints.DecimalMax.message': 'must be less than or equal to {0}.',
         'javax.validation.constraints.DecimalMin.message': 'must be greater than or equal to {0}.',
         'javax.validation.constraints.Digits.message': 'numeric value out of bounds (&lt;{0} digits&gt;.&lt;{1} digits&gt; expected).',
         'javax.validation.constraints.Future.message': 'must be in the future.',
         'javax.validation.constraints.Max.message': 'must be less than or equal to {0}.',
         'javax.validation.constraints.Min.message': 'must be greater than or equal to {0}.',
         'javax.validation.constraints.NotNull.message': 'may not be null.',
         'javax.validation.constraints.Null.message': 'must be null.',
         'javax.validation.constraints.Past.message': 'must be in the past.',
         'javax.validation.constraints.Pattern.message': 'must match "{0}".',
         'javax.validation.constraints.Size.message': 'size must be between {0} and {1}.'
     }
};
PrimeFaces.locales['en_US'] = PrimeFaces.locales['en'];
