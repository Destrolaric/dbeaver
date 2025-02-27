
/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.app.standalone;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.branding.IProductConstants;
import org.eclipse.ui.splash.BasicSplashHandler;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

/**
 * @since 3.3
 * 
 */
public class DBeaverSplashHandler extends BasicSplashHandler {

	public static final int TOTAL_LOADING_TASKS = 20;
	private static DBeaverSplashHandler instance;

    public static IProgressMonitor getActiveMonitor()
    {
        if (instance == null) {
            return null;
        } else {
            try {
                return instance.getBundleProgressMonitor();
            } catch (Exception e) {
                e.printStackTrace(System.err);
                return null;
            }
        }
    }

    private Font normalFont;
    private Font boldFont;

    public DBeaverSplashHandler()
    {
        instance = this;
    }

    @Override
    public void init(Shell splash) {
        super.init(splash);

        // https://github.com/eclipse-platform/eclipse.platform.swt/issues/772
        if (RuntimeUtils.isMacOS() && RuntimeUtils.isOSVersionAtLeast(14, 0, 0)) {
            return;
        }

        try {
            initVisualization();

            getBundleProgressMonitor().beginTask("Loading", TOTAL_LOADING_TASKS);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

    }
    
    @Override
    public IProgressMonitor getBundleProgressMonitor() {
        // https://github.com/eclipse-platform/eclipse.platform.swt/issues/772
        if (RuntimeUtils.isMacOS() && RuntimeUtils.isOSVersionAtLeast(14, 0, 0)) {
            return null;
        }
        return super.getBundleProgressMonitor();
    }

    private void initVisualization() {
        String progressRectString = null, messageRectString = null, foregroundColorString = null,
            versionCoordString = null, versionInfoSizeString = null;
        final IProduct product = Platform.getProduct();
        if (product != null) {
            progressRectString = product.getProperty(IProductConstants.STARTUP_PROGRESS_RECT);
            messageRectString = product.getProperty(IProductConstants.STARTUP_MESSAGE_RECT);
            versionCoordString = product.getProperty("versionInfoCoord");
            versionInfoSizeString = product.getProperty("versionInfoSize");
        }

        setProgressRect(StringConverter.asRectangle(progressRectString, new Rectangle(275, 300, 280, 10)));
        setMessageRect(StringConverter.asRectangle(messageRectString, new Rectangle(275,275,280,25)));
        final Point versionCoord = StringConverter.asPoint(versionCoordString, new Point(485, 215));
        final int versionInfoSize = StringConverter.asInt(versionInfoSizeString, 22);
        final RGB versionInfoRGB = new RGB(255,255,255);

        int foregroundColorInteger = 0xFFFFFF;

        setForeground(
			new RGB(
				(foregroundColorInteger & 0xFF0000) >> 16,
                (foregroundColorInteger & 0xFF00) >> 8,
                foregroundColorInteger & 0xFF));

        normalFont = getContent().getFont();
        //boldFont = UIUtils.makeBoldFont(normalFont);
        FontData[] fontData = normalFont.getFontData();
        fontData[0].setStyle(fontData[0].getStyle() | SWT.BOLD);
        fontData[0].setHeight(versionInfoSize);
        boldFont = new Font(normalFont.getDevice(), fontData[0]);

        final Color versionColor = new Color(getContent().getDisplay(), versionInfoRGB);

        getContent().addPaintListener(e -> {
            String productVersion = "";
            if (product != null) {
                productVersion = GeneralUtils.getPlainVersion();
            }
            //String osVersion = Platform.getOS() + " " + Platform.getOSArch();
            if (boldFont != null) {
                e.gc.setFont(boldFont);
            }
            e.gc.setForeground(versionColor);
            e.gc.drawText(productVersion, versionCoord.x, versionCoord.y, true);
            //e.gc.drawText(osVersion, 115, 200, true);
            e.gc.setFont(normalFont);
        });
    }

    @Override
    public void dispose()
    {
        super.dispose();
        if (boldFont != null) {
            boldFont.dispose();
            boldFont = null;
        }
        instance = null;
    }

	public static void showMessage(String message) {
        IProgressMonitor activeMonitor = getActiveMonitor();
		if (activeMonitor == null || message == null || message.isEmpty()) {
			return;
		}
		if (message.startsWith(">") || message.startsWith("<")) {
            message = message.substring(2);
            int divPos = message.indexOf("[");
            if (divPos != -1) {
                message = message.substring(0, divPos);
            }
        }
        try {
            activeMonitor.setTaskName(message);
            activeMonitor.worked(1);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
    }

}
