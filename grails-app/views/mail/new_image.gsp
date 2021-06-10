<g:render template="/mail/header" model="[]"/>

<!-- BODY -->
<table class="body-wrap">
    <tr>
        <td></td>
        <td class="container" bgcolor="#FFFFFF">
            <div class="content">
                <table class="social" width="100%">
                    <tr>
                        <td>
                            <div class="rv_mail">
                                <p class="rv_font">
                                    The image <%= abstractImageFilename %> is now available on ImageDx.<br/>
                                    <ul>
                                        <g:each in="${imagesInstances}">
                                            <li>Click <a href="${it.urlImageInstance}">here</a> to visualize <%= abstractImageFilename %> in workspace ${it.projectName}</li>
                                        </g:each>
                                    </ul>
                                </p>

                                <p class="rv_font">
                                    <img alt="<%= abstractImageFilename %>" src='cid:<%= cid %>' style="max-width: 400px; max-height: 400px;"/>
                                </p>
                            </div>
                            <!-- social & contact -->
                            <g:render template="/mail/social" model="[website :website, mailFrom: mailFrom, phoneNumber:phoneNumber]"/>

                        </td>
                    </tr>
                </table>
            </div><!-- /content -->
        </td>
        <td></td>
    </tr>
</table><!-- /BODY -->

<g:render template="/mail/footer" model="[by : by]"/>
